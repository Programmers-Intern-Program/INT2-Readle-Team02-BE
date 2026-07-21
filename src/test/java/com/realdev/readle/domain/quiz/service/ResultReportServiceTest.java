package com.realdev.readle.domain.quiz.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.realdev.readle.domain.quiz.dto.request.ResultReportSort;
import com.realdev.readle.domain.quiz.dto.response.ResultReportHistoryResponse;
import com.realdev.readle.domain.quiz.repository.ResultReportQueryRepository;
import com.realdev.readle.global.exception.CustomException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResultReportServiceTest {

  @Mock private ResultReportQueryRepository resultReportQueryRepository;
  @InjectMocks private ResultReportService resultReportService;

  @Test
  @DisplayName("정렬 문자열을 변환해 학습 히스토리 조회를 위임한다")
  void getHistory_DelegatesWithParsedSort() {
    ResultReportHistoryResponse response = ResultReportHistoryResponse.of(List.of(), 0, 10, 0);
    given(
            resultReportQueryRepository.findHistory(
                "member-uuid", 0, 10, ResultReportSort.LATEST, 31L))
        .willReturn(response);

    resultReportService.getHistory("member-uuid", 0, 10, "latest", 31L);

    then(resultReportQueryRepository)
        .should()
        .findHistory("member-uuid", 0, 10, ResultReportSort.LATEST, 31L);
  }

  @Test
  @DisplayName("지원하지 않는 정렬 조건은 잘못된 입력으로 거절한다")
  void getHistory_RejectsUnsupportedSort() {
    assertThatThrownBy(() -> resultReportService.getHistory("member-uuid", 0, 10, "popular", null))
        .isInstanceOf(CustomException.class)
        .hasMessageContaining("latest 또는 oldest");

    then(resultReportQueryRepository).shouldHaveNoInteractions();
  }
}
