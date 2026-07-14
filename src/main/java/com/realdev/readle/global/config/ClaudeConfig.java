package com.realdev.readle.global.config;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(ClaudeProperties.class)
public class ClaudeConfig {

  @Bean
  public RestClientCustomizer claudeRestClientCustomizer(ClaudeProperties properties) {
    return restClientBuilder -> {
      HttpClient httpClient =
          HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

      JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
      requestFactory.setReadTimeout(Duration.ofSeconds(30));

      restClientBuilder
          .requestFactory(requestFactory)
          .baseUrl(properties.getBaseUrl())
          .defaultHeader("x-api-key", properties.getApiKey())
          .defaultHeader("anthropic-version", properties.getVersion())
          .defaultHeader("content-type", MediaType.APPLICATION_JSON_VALUE);
    };
  }

  @Bean
  public RestClient claudeRestClient(RestClient.Builder restClientBuilder) {
    return restClientBuilder.build();
  }
}
