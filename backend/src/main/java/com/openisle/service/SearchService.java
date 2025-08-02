package com.openisle.service;

import com.openisle.model.Post;
import com.openisle.model.PostStatus;
import com.openisle.model.Comment;
import com.openisle.model.User;
import com.openisle.model.Category;
import com.openisle.model.Tag;
import com.openisle.repository.PostRepository;
import com.openisle.repository.CommentRepository;
import com.openisle.repository.UserRepository;
import com.openisle.repository.CategoryRepository;
import com.openisle.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.stream.Stream;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchService {
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;

    @org.springframework.beans.factory.annotation.Value("${app.snippet-length:50}")
    private int snippetLength;

    public List<User> searchUsers(String keyword) {
        return userRepository.findByUsernameContainingIgnoreCase(keyword);
    }

    public List<Post> searchPosts(String keyword) {
        return postRepository
                .findByTitleContainingIgnoreCaseOrContentContainingIgnoreCaseAndStatus(keyword, keyword, PostStatus.PUBLISHED);
    }

    public List<Post> searchPostsByContent(String keyword) {
        return postRepository
                .findByContentContainingIgnoreCaseAndStatus(keyword, PostStatus.PUBLISHED);
    }

    public List<Post> searchPostsByTitle(String keyword) {
        return postRepository
                .findByTitleContainingIgnoreCaseAndStatus(keyword, PostStatus.PUBLISHED);
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
        Stream<SearchResult> users = searchUsers(keyword).stream()
                .map(u -> new SearchResult(
                        "user",
                        u.getId(),
                        u.getUsername(),
                        u.getIntroduction(),
                        null,
                        null
                ));

        Stream<SearchResult> categories = searchCategories(keyword).stream()
                .map(c -> new SearchResult(
                        "category",
                        c.getId(),
                        c.getName(),
                        null,
                        c.getDescription(),
                        null
                ));

        Stream<SearchResult> tags = searchTags(keyword).stream()
                .map(t -> new SearchResult(
                        "tag",
                        t.getId(),
                        t.getName(),
                        null,
                        t.getDescription(),
                        null
                ));

        // Merge post results while removing duplicates between search by content
        // and search by title
        List<SearchResult> mergedPosts = Stream.concat(
                    searchPosts(keyword).stream()
                            .map(p -> new SearchResult(
                                    "post",
                                    p.getId(),
                                    p.getTitle(),
                                    p.getCategory() != null ? p.getCategory().getName() : null,
                                    extractSnippet(p.getContent(), keyword, false),
                                    null
                            )),
                    searchPostsByTitle(keyword).stream()
                            .map(p -> new SearchResult(
                                    "post_title",
                                    p.getId(),
                                    p.getTitle(),
                                    p.getCategory() != null ? p.getCategory().getName() : null,
                                    extractSnippet(p.getContent(), keyword, true),
                                    null
                            ))
                )
                .collect(java.util.stream.Collectors.toMap(
                        SearchResult::id,
                        sr -> sr,
                        (a, b) -> a,
                        java.util.LinkedHashMap::new
                ))
                .values()
                .stream()
                .toList();

        Stream<SearchResult> comments = searchComments(keyword).stream()
                .map(c -> new SearchResult(
                        "comment",
                        c.getId(),
                        c.getPost().getTitle(),
                        c.getAuthor().getUsername(),
                        extractSnippet(c.getContent(), keyword, false),
                        c.getPost().getId()
                ));

        return Stream.of(users, categories, tags, mergedPosts.stream(), comments)
                .flatMap(s -> s)
                .toList();
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

    public record SearchResult(String type, Long id, String text, String subText, String extra, Long postId) {}
}
