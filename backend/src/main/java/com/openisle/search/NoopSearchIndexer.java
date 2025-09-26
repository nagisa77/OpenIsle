package com.openisle.search;

public class NoopSearchIndexer implements SearchIndexer {

  @Override
  public void indexDocument(String index, SearchDocument document) {
    // no-op
  }

  @Override
  public void deleteDocument(String index, Long id) {
    // no-op
  }
}
