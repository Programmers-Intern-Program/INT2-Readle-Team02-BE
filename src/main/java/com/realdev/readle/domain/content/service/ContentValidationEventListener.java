package com.realdev.readle.domain.content.service;

import com.realdev.readle.domain.content.entity.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContentValidationEventListener {
  private final ContentValidationService contentValidationService;

  @Async("validationExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleContentCreatedEvent(ContentCreatedEvent event) {
    log.info("[VALIDATION_EVENT] 콘텐츠 검증 비동기 트리거 시작. Content ID: {}", event.contentId());
    try {
      contentValidationService.validateContent(event.contentId());
    } catch (Exception e) {
      log.error("[VALIDATION_EVENT] 콘텐츠 검증 진행 중 예외 발생. Content ID: {}", event.contentId(), e);
      contentValidationService.markAsFailed(event.contentId(), ErrorCode.UNKNOWN_ERROR);
    }
  }
}
