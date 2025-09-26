package com.openisle.search.event;

public record DeleteDocumentEvent(String index, Long id) {}
