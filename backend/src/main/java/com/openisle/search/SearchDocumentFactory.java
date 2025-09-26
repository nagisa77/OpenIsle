package com.openisle.search;

import com.openisle.model.Category;
import com.openisle.model.Comment;
import com.openisle.model.Post;
import com.openisle.model.Tag;
import com.openisle.model.User;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class SearchDocumentFactory {

  private SearchDocumentFactory() {}

  public static SearchDocument fromPost(Post post) {
    if (post == null || post.getId() == null) {
      return null;
    }
    List<String> tags = post.getTags() == null
      ? Collections.emptyList()
      : post
        .getTags()
        .stream()
        .map(Tag::getName)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
    return new SearchDocument(
      "post",
      post.getId(),
      post.getTitle(),
      post.getContent(),
      post.getAuthor() != null ? post.getAuthor().getUsername() : null,
      post.getCategory() != null ? post.getCategory().getName() : null,
      tags,
      post.getId(),
      toEpochMillis(post.getCreatedAt())
    );
  }

  public static SearchDocument fromComment(Comment comment) {
    if (comment == null || comment.getId() == null) {
      return null;
    }
    Post post = comment.getPost();
    List<String> tags = post == null || post.getTags() == null
      ? Collections.emptyList()
      : post
        .getTags()
        .stream()
        .map(Tag::getName)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
    return new SearchDocument(
      "comment",
      comment.getId(),
      post != null ? post.getTitle() : null,
      comment.getContent(),
      comment.getAuthor() != null ? comment.getAuthor().getUsername() : null,
      post != null && post.getCategory() != null ? post.getCategory().getName() : null,
      tags,
      post != null ? post.getId() : null,
      toEpochMillis(comment.getCreatedAt())
    );
  }

  public static SearchDocument fromUser(User user) {
    if (user == null || user.getId() == null) {
      return null;
    }
    return new SearchDocument(
      "user",
      user.getId(),
      user.getUsername(),
      user.getIntroduction(),
      null,
      null,
      Collections.emptyList(),
      null,
      toEpochMillis(user.getCreatedAt())
    );
  }

  public static SearchDocument fromCategory(Category category) {
    if (category == null || category.getId() == null) {
      return null;
    }
    return new SearchDocument(
      "category",
      category.getId(),
      category.getName(),
      category.getDescription(),
      null,
      null,
      Collections.emptyList(),
      null,
      null
    );
  }

  public static SearchDocument fromTag(Tag tag) {
    if (tag == null || tag.getId() == null) {
      return null;
    }
    return new SearchDocument(
      "tag",
      tag.getId(),
      tag.getName(),
      tag.getDescription(),
      null,
      null,
      Collections.emptyList(),
      null,
      toEpochMillis(tag.getCreatedAt())
    );
  }

  private static Long toEpochMillis(LocalDateTime time) {
    if (time == null) {
      return null;
    }
    return time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
  }
}
