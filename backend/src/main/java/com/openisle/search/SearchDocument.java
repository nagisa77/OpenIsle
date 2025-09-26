package com.openisle.search;

import java.util.List;

public record SearchDocument(
  String type,
  Long entityId,
  String title,
  String content,
  String author,
  String category,
  List<String> tags,
  Long postId,
  Long createdAt
) {}
