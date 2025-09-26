package com.openisle.search;

import com.openisle.model.Post;
import com.openisle.model.PostStatus;
import com.openisle.model.Tag;
import com.openisle.repository.CategoryRepository;
import com.openisle.repository.CommentRepository;
import com.openisle.repository.PostRepository;
import com.openisle.repository.TagRepository;
import com.openisle.repository.UserRepository;
import java.util.Objects;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class SearchReindexService {

  private final SearchIndexer searchIndexer;
  private final OpenSearchProperties properties;
  private final PostRepository postRepository;
  private final CommentRepository commentRepository;
  private final UserRepository userRepository;
  private final CategoryRepository categoryRepository;
  private final TagRepository tagRepository;

  @Transactional(readOnly = true)
  public void reindexAll() {
    if (!properties.isEnabled()) {
      log.info("Search indexing is disabled, skipping reindex operation.");
      return;
    }

    log.info("Starting full search reindex operation.");

    reindex(properties.postsIndex(), postRepository::findAll, (Post post) ->
      post.getStatus() == PostStatus.PUBLISHED ? SearchDocumentFactory.fromPost(post) : null
    );

    reindex(
      properties.commentsIndex(),
      commentRepository::findAll,
      SearchDocumentFactory::fromComment
    );

    reindex(properties.usersIndex(), userRepository::findAll, SearchDocumentFactory::fromUser);

    reindex(
      properties.categoriesIndex(),
      categoryRepository::findAll,
      SearchDocumentFactory::fromCategory
    );

    reindex(properties.tagsIndex(), tagRepository::findAll, (Tag tag) ->
      tag.isApproved() ? SearchDocumentFactory.fromTag(tag) : null
    );

    log.info("Completed full search reindex operation.");
  }

  private <T> void reindex(
    String index,
    Function<Pageable, Page<T>> pageSupplier,
    Function<T, SearchDocument> mapper
  ) {
    int batchSize = Math.max(1, properties.getReindexBatchSize());
    int pageNumber = 0;

    Page<T> page;
    do {
      Pageable pageable = PageRequest.of(pageNumber, batchSize);
      page = pageSupplier.apply(pageable);
      if (page.isEmpty() && pageNumber == 0) {
        log.info("No entities found for index {}.", index);
      }

      log.info("Reindexing {} entities for index {}.", page.getTotalElements(), index);
      for (T entity : page) {
        SearchDocument document = mapper.apply(entity);
        if (Objects.nonNull(document)) {
          searchIndexer.indexDocument(index, document);
        }
      }
      pageNumber++;
    } while (page.hasNext());
  }
}
