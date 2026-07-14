package com.realdev.readle.global.infrastructure.ai;

import com.realdev.readle.global.config.ClaudeProperties;
import com.realdev.readle.global.infrastructure.ai.dto.ClaudeRequest;
import com.realdev.readle.global.infrastructure.ai.dto.ClaudeResponse;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class ClaudeClient {

  private static final Logger log = LoggerFactory.getLogger(ClaudeClient.class);

  private final RestClient claudeRestClient;
  private final ClaudeProperties properties;

  public ClaudeClient(RestClient claudeRestClient, ClaudeProperties properties) {
    this.claudeRestClient = claudeRestClient;
    this.properties = properties;
  }

  // 기본 모델(properties.getModel())을 사용하는 기본 버전
  public ClaudeResponse generateMessage(String systemPrompt, String userPrompt) {
    return generateMessage(properties.getModel(), systemPrompt, userPrompt);
  }

  // 외부 주입된 가변 모델명을 사용하며 1회 재시도 가드가 적용된 오버로딩 버전
  public ClaudeResponse generateMessage(String model, String systemPrompt, String userPrompt) {
    try {
      return executeGenerateMessage(model, systemPrompt, userPrompt);
    } catch (RestClientException e) {
      log.warn("[CLAUDE_API_WARNING] Claude API 최초 호출 실패. 1회 재시도를 진행합니다. 에러: {}", e.getMessage());
      try {
        // 딱 1회 백그라운드 재시도 정책 수행
        return executeGenerateMessage(model, systemPrompt, userPrompt);
      } catch (RestClientException retryEx) {
        log.error(
            "[CLAUDE_API_ERROR] Claude API 재시도 호출도 실패하였습니다. 최종 실패 처리합니다. 에러: {}",
            retryEx.getMessage(),
            retryEx);
        throw retryEx;
      }
    }
  }

  // 실제 HTTP POST 통신을 수행하는 내부 격리 메서드
  private ClaudeResponse executeGenerateMessage(
      String model, String systemPrompt, String userPrompt) {
    ClaudeRequest request =
        ClaudeRequest.builder()
            .model(model)
            .maxTokens(4000)
            .system(systemPrompt)
            .messages(
                List.of(ClaudeRequest.Message.builder().role("user").content(userPrompt).build()))
            .build();

    return claudeRestClient
        .post()
        .uri("/v1/messages")
        .body(request)
        .retrieve()
        .body(ClaudeResponse.class);
  }

  // 기본 모델(properties.getModel())을 사용하는 기본 버전
  public String getGeneratedText(String systemPrompt, String userPrompt) {
    return getGeneratedText(properties.getModel(), systemPrompt, userPrompt);
  }

  // 외부 주입된 가변 모델명을 사용하는 오버로딩 버전
  public String getGeneratedText(String model, String systemPrompt, String userPrompt) {
    ClaudeResponse response = generateMessage(model, systemPrompt, userPrompt);
    if (response == null || response.getContent() == null || response.getContent().isEmpty()) {
      throw new IllegalStateException("Claude API로부터 비어있는 응답을 받았습니다.");
    }
    return response.getContent().get(0).getText();
  }
}
