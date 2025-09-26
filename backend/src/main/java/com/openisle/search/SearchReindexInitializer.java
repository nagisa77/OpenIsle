package com.openisle.search;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SearchReindexInitializer implements CommandLineRunner {

  private final OpenSearchProperties properties;
  private final SearchReindexService searchReindexService;

  @Override
  public void run(String... args) {
    if (!properties.isEnabled()) {
      log.info("Search indexing disabled, skipping startup reindex.");
      return;
    }

    if (!properties.isReindexOnStartup()) {
      log.debug("Startup reindex disabled by configuration.");
      return;
    }

    searchReindexService.reindexAll();
  }
}
