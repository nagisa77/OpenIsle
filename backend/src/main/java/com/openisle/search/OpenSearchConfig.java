package com.openisle.search;

import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.core5.http.HttpHost;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestClientBuilder;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.rest_client.RestClientTransport;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@EnableConfigurationProperties(OpenSearchProperties.class)
public class OpenSearchConfig {

  @Bean(destroyMethod = "close")
  @ConditionalOnProperty(prefix = "app.search", name = "enabled", havingValue = "true")
  public RestClient openSearchRestClient(OpenSearchProperties properties) {
    RestClientBuilder builder = RestClient.builder(
      new HttpHost(properties.getScheme(), properties.getHost(), properties.getPort())
    );
    if (StringUtils.hasText(properties.getUsername())) {
      BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
      credentialsProvider.setCredentials(
        new AuthScope(properties.getHost(), properties.getPort()),
        new UsernamePasswordCredentials(
          properties.getUsername(),
          properties.getPassword() != null ? properties.getPassword().toCharArray() : new char[0]
        )
      );
      builder.setHttpClientConfigCallback(httpClientBuilder ->
        httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
      );
    }
    return builder.build();
  }

  @Bean(destroyMethod = "close")
  @ConditionalOnBean(RestClient.class)
  public RestClientTransport openSearchTransport(RestClient restClient) {
    return new RestClientTransport(restClient, new JacksonJsonpMapper());
  }

  @Bean
  @ConditionalOnBean(RestClientTransport.class)
  public OpenSearchClient openSearchClient(RestClientTransport transport) {
    return new OpenSearchClient(transport);
  }

  @Bean
  @ConditionalOnBean(OpenSearchClient.class)
  public SearchIndexInitializer searchIndexInitializer(
    OpenSearchClient client,
    OpenSearchProperties properties
  ) {
    return new SearchIndexInitializer(client, properties);
  }

  @Bean
  @ConditionalOnBean(OpenSearchClient.class)
  public SearchIndexer openSearchIndexer(OpenSearchClient client, OpenSearchProperties properties) {
    return new OpenSearchIndexer(client);
  }

  @Bean
  @ConditionalOnMissingBean(SearchIndexer.class)
  public SearchIndexer noopSearchIndexer() {
    return new NoopSearchIndexer();
  }
}
