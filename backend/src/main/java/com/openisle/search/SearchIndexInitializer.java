package com.openisle.search;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.mapping.Property;
import org.opensearch.client.opensearch._types.mapping.TypeMapping;

@Slf4j
@RequiredArgsConstructor
public class SearchIndexInitializer {

  private final OpenSearchClient client;
  private final OpenSearchProperties properties;

  @PostConstruct
  public void initialize() {
    if (!properties.isEnabled() || !properties.isInitialize()) {
      return;
    }
    ensureIndex(properties.postsIndex(), this::postMapping);
    ensureIndex(properties.commentsIndex(), this::commentMapping);
    ensureIndex(properties.usersIndex(), this::userMapping);
    ensureIndex(properties.categoriesIndex(), this::categoryMapping);
    ensureIndex(properties.tagsIndex(), this::tagMapping);
  }

  private void ensureIndex(String index, java.util.function.Supplier<TypeMapping> mappingSupplier) {
    try {
      boolean exists = client
        .indices()
        .exists(builder -> builder.index(index))
        .value();
      if (exists) {
        return;
      }
      client.indices().create(builder -> builder.index(index).mappings(mappingSupplier.get()));
      log.info("Created OpenSearch index {}", index);
    } catch (IOException e) {
      log.warn("Failed to initialize OpenSearch index {}", index, e);
    }
  }

  private TypeMapping postMapping() {
    return TypeMapping.of(builder ->
      builder
        .properties("type", Property.of(p -> p.keyword(k -> k)))
        .properties("title", Property.of(p -> p.text(t -> t)))
        .properties("content", Property.of(p -> p.text(t -> t)))
        .properties("author", Property.of(p -> p.keyword(k -> k)))
        .properties("category", Property.of(p -> p.keyword(k -> k)))
        .properties("tags", Property.of(p -> p.keyword(k -> k)))
        .properties("postId", Property.of(p -> p.long_(l -> l)))
        .properties(
          "createdAt",
          Property.of(p -> p.date(d -> d.format("strict_date_optional_time||epoch_millis")))
        )
    );
  }

  private TypeMapping commentMapping() {
    return TypeMapping.of(builder ->
      builder
        .properties("type", Property.of(p -> p.keyword(k -> k)))
        .properties("title", Property.of(p -> p.text(t -> t)))
        .properties("content", Property.of(p -> p.text(t -> t)))
        .properties("author", Property.of(p -> p.keyword(k -> k)))
        .properties("category", Property.of(p -> p.keyword(k -> k)))
        .properties("tags", Property.of(p -> p.keyword(k -> k)))
        .properties("postId", Property.of(p -> p.long_(l -> l)))
        .properties(
          "createdAt",
          Property.of(p -> p.date(d -> d.format("strict_date_optional_time||epoch_millis")))
        )
    );
  }

  private TypeMapping userMapping() {
    return TypeMapping.of(builder ->
      builder
        .properties("type", Property.of(p -> p.keyword(k -> k)))
        .properties("title", Property.of(p -> p.text(t -> t)))
        .properties("content", Property.of(p -> p.text(t -> t)))
        .properties(
          "createdAt",
          Property.of(p -> p.date(d -> d.format("strict_date_optional_time||epoch_millis")))
        )
    );
  }

  private TypeMapping categoryMapping() {
    return TypeMapping.of(builder ->
      builder
        .properties("type", Property.of(p -> p.keyword(k -> k)))
        .properties("title", Property.of(p -> p.text(t -> t)))
        .properties("content", Property.of(p -> p.text(t -> t)))
    );
  }

  private TypeMapping tagMapping() {
    return TypeMapping.of(builder ->
      builder
        .properties("type", Property.of(p -> p.keyword(k -> k)))
        .properties("title", Property.of(p -> p.text(t -> t)))
        .properties("content", Property.of(p -> p.text(t -> t)))
        .properties(
          "createdAt",
          Property.of(p -> p.date(d -> d.format("strict_date_optional_time||epoch_millis")))
        )
    );
  }
}
