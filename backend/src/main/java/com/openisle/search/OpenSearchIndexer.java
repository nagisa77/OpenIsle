package com.openisle.search;

import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.DeleteRequest;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.opensearch.client.opensearch.core.IndexResponse;

@Slf4j
@RequiredArgsConstructor
public class OpenSearchIndexer implements SearchIndexer {

  private final OpenSearchClient client;

  @Override
  public void indexDocument(String index, SearchDocument document) {
    if (document == null || document.entityId() == null) {
      return;
    }
    try {
      IndexRequest<SearchDocument> request = IndexRequest.of(builder ->
        builder.index(index).id(document.entityId().toString()).document(document)
      );
      IndexResponse response = client.index(request);
      log.info(
        "Indexed document {} into {} with result {}",
        document.entityId(),
        index,
        response.result()
      );
    } catch (IOException e) {
      log.warn("Failed to index document {} into {}", document.entityId(), index, e);
    }
  }

  @Override
  public void deleteDocument(String index, Long id) {
    if (id == null) {
      return;
    }
    try {
      client.delete(DeleteRequest.of(builder -> builder.index(index).id(id.toString())));
    } catch (IOException e) {
      log.warn("Failed to delete document {} from {}", id, index, e);
    }
  }
}
