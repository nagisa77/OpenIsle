package com.openisle.search;

import com.openisle.model.Category;
import com.openisle.model.Comment;
import com.openisle.model.Post;
import com.openisle.model.PostStatus;
import com.openisle.model.Tag;
import com.openisle.model.User;
import com.openisle.search.event.DeleteDocumentEvent;
import com.openisle.search.event.IndexDocumentEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SearchIndexEventPublisher {

  private final ApplicationEventPublisher publisher;
  private final OpenSearchProperties properties;

  public void publishPostSaved(Post post) {
    if (!properties.isEnabled() || post == null || post.getStatus() != PostStatus.PUBLISHED) {
      return;
    }
    SearchDocument document = SearchDocumentFactory.fromPost(post);
    if (document != null) {
      publisher.publishEvent(new IndexDocumentEvent(properties.postsIndex(), document));
    }
  }

  public void publishPostDeleted(Long postId) {
    if (!properties.isEnabled() || postId == null) {
      return;
    }
    publisher.publishEvent(new DeleteDocumentEvent(properties.postsIndex(), postId));
  }

  public void publishCommentSaved(Comment comment) {
    if (!properties.isEnabled() || comment == null) {
      return;
    }
    SearchDocument document = SearchDocumentFactory.fromComment(comment);
    if (document != null) {
      publisher.publishEvent(new IndexDocumentEvent(properties.commentsIndex(), document));
    }
  }

  public void publishCommentDeleted(Long commentId) {
    if (!properties.isEnabled() || commentId == null) {
      return;
    }
    publisher.publishEvent(new DeleteDocumentEvent(properties.commentsIndex(), commentId));
  }

  public void publishUserSaved(User user) {
    if (!properties.isEnabled() || user == null) {
      return;
    }
    SearchDocument document = SearchDocumentFactory.fromUser(user);
    if (document != null) {
      publisher.publishEvent(new IndexDocumentEvent(properties.usersIndex(), document));
    }
  }

  public void publishCategorySaved(Category category) {
    if (!properties.isEnabled() || category == null) {
      return;
    }
    SearchDocument document = SearchDocumentFactory.fromCategory(category);
    if (document != null) {
      publisher.publishEvent(new IndexDocumentEvent(properties.categoriesIndex(), document));
    }
  }

  public void publishCategoryDeleted(Long categoryId) {
    if (!properties.isEnabled() || categoryId == null) {
      return;
    }
    publisher.publishEvent(new DeleteDocumentEvent(properties.categoriesIndex(), categoryId));
  }

  public void publishTagSaved(Tag tag) {
    if (!properties.isEnabled() || tag == null || !tag.isApproved()) {
      return;
    }
    SearchDocument document = SearchDocumentFactory.fromTag(tag);
    if (document != null) {
      publisher.publishEvent(new IndexDocumentEvent(properties.tagsIndex(), document));
    }
  }

  public void publishTagDeleted(Long tagId) {
    if (!properties.isEnabled() || tagId == null) {
      return;
    }
    publisher.publishEvent(new DeleteDocumentEvent(properties.tagsIndex(), tagId));
  }
}
