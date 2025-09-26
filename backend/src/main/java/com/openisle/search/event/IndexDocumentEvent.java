package com.openisle.search.event;

import com.openisle.search.SearchDocument;

public record IndexDocumentEvent(String index, SearchDocument document) {}
