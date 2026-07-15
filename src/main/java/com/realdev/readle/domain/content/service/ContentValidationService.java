package com.realdev.readle.domain.content.service;

import com.realdev.readle.domain.content.entity.*;
import com.realdev.readle.domain.content.exception.ContentErrorCode;
import com.realdev.readle.domain.content.repository.ContentRepository;
import com.realdev.readle.domain.content.repository.ContentValidationRepository;
import com.realdev.readle.global.exception.CustomException;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentValidationService {

  private final ContentRepository contentRepository;
  private final ContentValidationRepository contentValidationRepository;

  private final StaticGuardrailValidator staticGuardrailValidator;
  private final WhitelistValidator whitelistValidator;

  @Transactional
  public void validateContent(Long contentId) {
    Content content =
        contentRepository
            .findById(contentId)
            .orElseThrow(() -> new CustomException(ContentErrorCode.CONTENT_NOT_FOUND));

    // 1차 방어선: 정적 가드레일 검사
    Optional<RejectReasonCode> rejectReason = staticGuardrailValidator.validate(content);
    if (rejectReason.isPresent()) {
      saveStaticGuardrailResult(content, rejectReason.get());
      log.info("[VALIDATION] 1차 가드레일 차단 완료. Content ID: {}, 사유: {}", contentId, rejectReason.get());
      return;
    }
    // 2차 판단: 화이트리스트 프리패스 검사
    if (whitelistValidator.isEligibleForWhitelist(content)) {
      saveWhitelistResult(content);
      log.info("[VALIDATION] 2차 화이트리스트 패스 완료. Content ID: {}", contentId);
      return;
    }

    // 1, 2차 통과 시 -> 3차 AI 검증 진행 (비동기 I/O 처리를 위해 트랜잭션 1, 2 분리)
    // Step 4에서 구현할 AI 검증 서비스로 위임
  }

  private void saveStaticGuardrailResult(Content content, RejectReasonCode reasonCode) {
    ContentValidation validation =
        createValidation(
            content, ValidationMethod.STATIC_GUARDRAIL, ValidationStatus.REJECTED, reasonCode);
    contentValidationRepository.save(validation);
  }

  private void saveWhitelistResult(Content content) {
    ContentValidation validation =
        createValidation(content, ValidationMethod.WHITELIST, ValidationStatus.PASSED, null);
    contentValidationRepository.save(validation);
  }

  // ContentValidation 엔티티 생성 헬퍼
  private ContentValidation createValidation(
      Content content,
      ValidationMethod method,
      ValidationStatus status,
      RejectReasonCode reasonCode) {

    return ContentValidation.builder()
        .content(content)
        .validationMethod(method)
        .status(status)
        .rejectReasonCode(reasonCode)
        .validatedAt(LocalDateTime.now())
        .build();
  }

  @Transactional
  public void markAsFailed(Long contentId, ErrorCode errorCode) {
    contentRepository
        .findById(contentId)
        .ifPresent(
            content -> {
              ContentValidation validation =
                  ContentValidation.builder()
                      .content(content)
                      .validationMethod(ValidationMethod.STATIC_GUARDRAIL) // 혹은 별도 UNKNOWN 값 검토
                      .status(ValidationStatus.FAILED)
                      .errorCode(errorCode)
                      .validatedAt(LocalDateTime.now())
                      .build();
              contentValidationRepository.save(validation);
            });
  }
}
