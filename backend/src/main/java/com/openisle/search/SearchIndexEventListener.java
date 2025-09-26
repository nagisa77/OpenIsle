package com.openisle.search;

import com.openisle.search.event.DeleteDocumentEvent;
import com.openisle.search.event.IndexDocumentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class SearchIndexEventListener {

  private final SearchIndexer searchIndexer;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void handleIndex(IndexDocumentEvent event) {
    if (event == null || event.document() == null) {
      return;
    }
    searchIndexer.indexDocument(event.index(), event.document());
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void handleDelete(DeleteDocumentEvent event) {
    if (event == null) {
      return;
    }
    searchIndexer.deleteDocument(event.index(), event.id());
  }
}
