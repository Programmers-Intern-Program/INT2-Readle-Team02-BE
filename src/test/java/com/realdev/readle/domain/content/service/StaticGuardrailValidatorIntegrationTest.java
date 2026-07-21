package com.realdev.readle.domain.content.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.realdev.readle.domain.content.entity.Content;
import com.realdev.readle.domain.content.entity.RejectReasonCode;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

@SpringBootTest
class StaticGuardrailValidatorIntegrationTest {

  @Autowired private StaticGuardrailValidator staticGuardrailValidator;

  @Test
  @DisplayName("StaticGuardrailValidator 빈이 정상적으로 주입되고, 내부의 safeWords 필드가 올바르게 할당된다")
  void shouldInjectBeanSuccessfully() {
    // 1. 빈 자체가 정상 주입되었는지 확인
    assertThat(staticGuardrailValidator).isNotNull();

    // 2. 내부의 safeWords 필드가 Null이 아니고 정상적으로 List 빈으로 주입되었는지 확인
    @SuppressWarnings("unchecked")
    List<String> safeWords =
        (List<String>) ReflectionTestUtils.getField(staticGuardrailValidator, "safeWords");

    assertThat(safeWords).isNotNull();
  }

  @Test
  @DisplayName("스프링 컨텍스트 환경에서 validate 호출 시 실제 빈들이 연동되어 정상 동작한다")
  void validate_withRealBeans_worksCorrectly() {
    // given
    Content validContent = mock(Content.class);
    String longText = "이것은 정상적인 길이의 안전한 텍스트입니다. 테스트를 위한 더미 데이터입니다. ".repeat(10);
    when(validContent.getRawText()).thenReturn(longText);
    when(validContent.getExtractedText()).thenReturn(longText);

    // when
    Optional<RejectReasonCode> result = staticGuardrailValidator.validate(validContent);

    // then
    // 정상 텍스트의 경우 비속어나 짧은 길이에 걸리지 않으므로 empty 반환
    assertThat(result).isEmpty();
  }
}
