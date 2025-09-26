package com.openisle.search;

public interface SearchIndexer {
  void indexDocument(String index, SearchDocument document);
  void deleteDocument(String index, Long id);
}
