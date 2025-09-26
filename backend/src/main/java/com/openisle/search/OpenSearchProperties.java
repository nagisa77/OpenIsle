package com.openisle.search;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.search")
public class OpenSearchProperties {

  private boolean enabled = false;
  private String host = "localhost";
  private int port = 9200;
  private String scheme = "http";
  private String username;
  private String password;
  private String indexPrefix = "openisle";
  private boolean initialize = true;
  private int highlightFragmentSize = 200;
  private boolean reindexOnStartup = false;
  private int reindexBatchSize = 500;

  private Indices indices = new Indices();

  public String postsIndex() {
    return indexName(indices.posts);
  }

  public String commentsIndex() {
    return indexName(indices.comments);
  }

  public String usersIndex() {
    return indexName(indices.users);
  }

  public String categoriesIndex() {
    return indexName(indices.categories);
  }

  public String tagsIndex() {
    return indexName(indices.tags);
  }

  private String indexName(String suffix) {
    if (indexPrefix == null || indexPrefix.isBlank()) {
      return suffix;
    }
    return indexPrefix + "-" + suffix;
  }

  @Getter
  @Setter
  public static class Indices {

    private String posts = "posts";
    private String comments = "comments";
    private String users = "users";
    private String categories = "categories";
    private String tags = "tags";
  }
}
