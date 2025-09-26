package com.openisle.service;

import com.openisle.model.Category;
import com.openisle.model.Comment;
import com.openisle.model.Post;
import com.openisle.model.PostStatus;
import com.openisle.model.Tag;
import com.openisle.model.User;
import com.openisle.repository.CategoryRepository;
import com.openisle.repository.CommentRepository;
import com.openisle.repository.PostRepository;
import com.openisle.repository.TagRepository;
import com.openisle.repository.UserRepository;
import com.openisle.search.OpenSearchProperties;
import com.openisle.search.SearchDocument;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.query_dsl.TextQueryType;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class SearchService {

  private final UserRepository userRepository;
  private final PostRepository postRepository;
  private final CommentRepository commentRepository;
  private final CategoryRepository categoryRepository;
  private final TagRepository tagRepository;
  private final Optional<OpenSearchClient> openSearchClient;
  private final OpenSearchProperties openSearchProperties;

  @org.springframework.beans.factory.annotation.Value("${app.snippet-length}")
  private int snippetLength;

  private static final int DEFAULT_OPEN_SEARCH_LIMIT = 50;

  public List<User> searchUsers(String keyword) {
    return userRepository.findByUsernameContainingIgnoreCase(keyword);
  }

  public List<Post> searchPosts(String keyword) {
    return postRepository.findByTitleContainingIgnoreCaseOrContentContainingIgnoreCaseAndStatus(
      keyword,
      keyword,
      PostStatus.PUBLISHED
    );
  }

  public List<Post> searchPostsByContent(String keyword) {
    return postRepository.findByContentContainingIgnoreCaseAndStatus(keyword, PostStatus.PUBLISHED);
  }

  public List<Post> searchPostsByTitle(String keyword) {
    return postRepository.findByTitleContainingIgnoreCaseAndStatus(keyword, PostStatus.PUBLISHED);
  }

  public List<Comment> searchComments(String keyword) {
    return commentRepository.findByContentContainingIgnoreCase(keyword);
  }

  public List<Category> searchCategories(String keyword) {
    return categoryRepository.findByNameContainingIgnoreCase(keyword);
  }

  public List<Tag> searchTags(String keyword) {
    return tagRepository.findByNameContainingIgnoreCaseAndApprovedTrue(keyword);
  }

  public List<SearchResult> globalSearch(String keyword) {
    if (keyword == null || keyword.isBlank()) {
      return List.of();
    }
    if (isOpenSearchEnabled()) {
      try {
        List<SearchResult> results = searchWithOpenSearch(keyword);
        if (!results.isEmpty()) {
          return results;
        }
      } catch (IOException e) {
        log.warn("OpenSearch global search failed, falling back to database query", e);
      }
    }
    return fallbackGlobalSearch(keyword);
  }

  private List<SearchResult> fallbackGlobalSearch(String keyword) {
    Stream<SearchResult> users = searchUsers(keyword)
      .stream()
      .map(u ->
        new SearchResult("user", u.getId(), u.getUsername(), u.getIntroduction(), null, null)
      );

    Stream<SearchResult> categories = searchCategories(keyword)
      .stream()
      .map(c ->
        new SearchResult("category", c.getId(), c.getName(), null, c.getDescription(), null)
      );

    Stream<SearchResult> tags = searchTags(keyword)
      .stream()
      .map(t -> new SearchResult("tag", t.getId(), t.getName(), null, t.getDescription(), null));

    // Merge post results while removing duplicates between search by content
    // and search by title
    List<SearchResult> mergedPosts = Stream.concat(
      searchPosts(keyword)
        .stream()
        .map(p ->
          new SearchResult(
            "post",
            p.getId(),
            p.getTitle(),
            p.getCategory() != null ? p.getCategory().getName() : null,
            extractSnippet(p.getContent(), keyword, false),
            null
          )
        ),
      searchPostsByTitle(keyword)
        .stream()
        .map(p ->
          new SearchResult(
            "post_title",
            p.getId(),
            p.getTitle(),
            p.getCategory() != null ? p.getCategory().getName() : null,
            extractSnippet(p.getContent(), keyword, true),
            null
          )
        )
    )
      .collect(
        java.util.stream.Collectors.toMap(
          SearchResult::id,
          sr -> sr,
          (a, b) -> a,
          java.util.LinkedHashMap::new
        )
      )
      .values()
      .stream()
      .toList();

    Stream<SearchResult> comments = searchComments(keyword)
      .stream()
      .map(c ->
        new SearchResult(
          "comment",
          c.getId(),
          c.getPost().getTitle(),
          c.getAuthor().getUsername(),
          extractSnippet(c.getContent(), keyword, false),
          c.getPost().getId()
        )
      );

    return Stream.of(users, categories, tags, mergedPosts.stream(), comments)
      .flatMap(s -> s)
      .toList();
  }

  private boolean isOpenSearchEnabled() {
    return openSearchProperties.isEnabled() && openSearchClient.isPresent();
  }

  private List<SearchResult> searchWithOpenSearch(String keyword) throws IOException {
    var client = openSearchClient.orElse(null);
    if (client == null) return List.of();

    final String qRaw = keyword == null ? "" : keyword.trim();
    if (qRaw.isEmpty()) return List.of();

    final boolean enableWildcard = qRaw.length() >= 2;
    final String qsEscaped = escapeForQueryString(qRaw);

    SearchResponse<SearchDocument> resp = client.search(
      b ->
        b
          .index(searchIndices())
          .trackTotalHits(t -> t.enabled(true))
          .query(qb ->
            qb.bool(bool -> {
              // 1) 主召回：title/content
              bool.should(s ->
                s.multiMatch(mm ->
                  mm
                    .query(qRaw)
                    .fields("title^3", "content^2")
                    .type(TextQueryType.BestFields)
                    .fuzziness("AUTO")
                    .minimumShouldMatch("70%")
                    .lenient(true)
                )
              );

              // 2) 兜底：open* 前缀命中
              if (enableWildcard) {
                bool.should(s ->
                  s.queryString(qs ->
                    qs
                      .query("(title:" + qsEscaped + "* OR content:" + qsEscaped + "*)")
                      .analyzeWildcard(true)
                  )
                );
              }

              // 3) 结构化字段（keyword）
              // term 需要 FieldValue（用 lambda 设置 stringValue）
              bool.should(s ->
                s.term(t ->
                  t
                    .field("author")
                    .value(v -> v.stringValue(qRaw))
                    .boost(2.0f)
                )
              );

              if (enableWildcard) {
                // prefix/wildcard 这里的 value 是 String，直接传即可
                bool.should(s -> s.prefix(p -> p.field("category").value(qRaw).boost(1.2f)));
                bool.should(s ->
                  s.wildcard(w -> w.field("category").value("*" + qRaw + "*").boost(1.0f))
                );

                bool.should(s ->
                  s.term(t ->
                    t
                      .field("tags")
                      .value(v -> v.stringValue(qRaw))
                      .boost(1.2f)
                  )
                );
                bool.should(s ->
                  s.wildcard(w -> w.field("tags").value("*" + qRaw + "*").boost(1.0f))
                );
              }

              return bool.minimumShouldMatch("1");
            })
          )
          .highlight(h ->
            h
              .preTags("<mark>")
              .postTags("</mark>")
              .fields("title", f -> f.fragmentSize(highlightFragmentSize()).numberOfFragments(1))
              .fields("content", f -> f.fragmentSize(highlightFragmentSize()).numberOfFragments(1))
          )
          .size(DEFAULT_OPEN_SEARCH_LIMIT > 0 ? DEFAULT_OPEN_SEARCH_LIMIT : 10),
      SearchDocument.class
    );

    return mapHits(resp.hits().hits(), qRaw);
  }

  /** Lucene query_string 安全转义（保留 * 由我们自己追加） */
  private static String escapeForQueryString(String s) {
    if (s == null || s.isEmpty()) return "";
    StringBuilder sb = new StringBuilder(s.length() * 2);
    for (char c : s.toCharArray()) {
      switch (c) {
        case '+':
        case '-':
        case '=':
        case '&':
        case '|':
        case '>':
        case '<':
        case '!':
        case '(':
        case ')':
        case '{':
        case '}':
        case '[':
        case ']':
        case '^':
        case '"':
        case '~': /* case '*': */ /* case '?': */
        case ':':
        case '\\':
        case '/':
          sb.append('\\').append(c);
          break;
        default:
          sb.append(c);
      }
    }
    return sb.toString();
  }

  private int highlightFragmentSize() {
    int configured = openSearchProperties.getHighlightFragmentSize();
    if (configured > 0) {
      return configured;
    }
    if (snippetLength > 0) {
      return snippetLength;
    }
    return 200;
  }

  private List<String> searchIndices() {
    return List.of(
      openSearchProperties.postsIndex(),
      openSearchProperties.commentsIndex(),
      openSearchProperties.usersIndex(),
      openSearchProperties.categoriesIndex(),
      openSearchProperties.tagsIndex()
    );
  }

  private List<SearchResult> mapHits(List<Hit<SearchDocument>> hits, String keyword) {
    List<SearchResult> results = new ArrayList<>();
    for (Hit<SearchDocument> hit : hits) {
      SearchResult result = mapHit(hit, keyword);
      if (result != null) {
        results.add(result);
      }
    }
    return results;
  }

  private SearchResult mapHit(Hit<SearchDocument> hit, String keyword) {
    SearchDocument document = hit.source();
    if (document == null || document.entityId() == null) {
      return null;
    }
    Map<String, List<String>> highlight = hit.highlight();
    String highlightedContent = firstHighlight(highlight, "content");
    String highlightedTitle = firstHighlight(highlight, "title");
    boolean highlightTitle = highlightedTitle != null && !highlightedTitle.isBlank();
    String documentType = document.type() != null ? document.type() : "";
    String effectiveType = documentType;
    if ("post".equals(documentType) && highlightTitle) {
      effectiveType = "post_title";
    }
    String snippet = highlightedContent != null && !highlightedContent.isBlank()
      ? cleanHighlight(highlightedContent)
      : null;
    if (snippet == null && highlightTitle) {
      snippet = cleanHighlight(highlightedTitle);
    }
    boolean fromStart = "post_title".equals(effectiveType);
    if (snippet == null || snippet.isBlank()) {
      snippet = fallbackSnippet(document.content(), keyword, fromStart);
    }
    if (snippet == null) {
      snippet = "";
    }
    String subText = null;
    Long postId = null;
    if ("post".equals(documentType) || "post_title".equals(effectiveType)) {
      subText = document.category();
    } else if ("comment".equals(documentType)) {
      subText = document.author();
      postId = document.postId();
    }
    return new SearchResult(
      effectiveType,
      document.entityId(),
      document.title(),
      subText,
      snippet,
      postId
    );
  }

  private String firstHighlight(Map<String, List<String>> highlight, String field) {
    if (highlight == null || field == null) {
      return null;
    }
    List<String> values = highlight.get(field);
    if (values == null || values.isEmpty()) {
      return null;
    }
    return values.get(0);
  }

  private String cleanHighlight(String value) {
    if (value == null) {
      return null;
    }
    return value.replaceAll("<[^>]+>", "");
  }

  private String fallbackSnippet(String content, String keyword, boolean fromStart) {
    if (content == null) {
      return "";
    }
    return extractSnippet(content, keyword, fromStart);
  }

  private String extractSnippet(String content, String keyword, boolean fromStart) {
    if (content == null) return "";
    int limit = snippetLength;
    if (fromStart) {
      if (limit < 0) {
        return content;
      }
      return content.length() > limit ? content.substring(0, limit) : content;
    }
    String lower = content.toLowerCase();
    String kw = keyword.toLowerCase();
    int idx = lower.indexOf(kw);
    if (idx == -1) {
      if (limit < 0) {
        return content;
      }
      return content.length() > limit ? content.substring(0, limit) : content;
    }
    int start = Math.max(0, idx - 20);
    int end = Math.min(content.length(), idx + kw.length() + 20);
    String snippet = content.substring(start, end);
    if (limit >= 0 && snippet.length() > limit) {
      snippet = snippet.substring(0, limit);
    }
    return snippet;
  }

  public record SearchResult(
    String type,
    Long id,
    String text,
    String subText,
    String extra,
    Long postId
  ) {}
}
