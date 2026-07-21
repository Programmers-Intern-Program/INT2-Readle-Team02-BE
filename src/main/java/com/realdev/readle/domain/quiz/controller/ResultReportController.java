package com.realdev.readle.domain.quiz.controller;

import com.realdev.readle.domain.quiz.dto.response.QuizAttemptResultResponse;
import com.realdev.readle.domain.quiz.dto.response.ResultReportHistoryResponse;
import com.realdev.readle.domain.quiz.service.QuizSolveService;
import com.realdev.readle.domain.quiz.service.ResultReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "ResultReport", description = "결과 리포트 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/result-reports")
public class ResultReportController {

  private final QuizSolveService quizSolveService;
  private final ResultReportService resultReportService;

  @Operation(summary = "학습 히스토리 목록 조회", description = "로그인 사용자의 제출 완료 결과 리포트를 조회합니다.")
  @GetMapping
  public ResponseEntity<ResultReportHistoryResponse> getResultReports(
      @AuthenticationPrincipal String memberUuid,
      @RequestParam(defaultValue = "0") @Min(value = 0, message = "page는 0 이상이어야 합니다.") int page,
      @RequestParam(defaultValue = "10")
          @Min(value = 1, message = "size는 1 이상이어야 합니다.") @Max(value = 50, message = "size는 50 이하여야 합니다.") int size,
      @RequestParam(defaultValue = "latest")
          @Pattern(regexp = "latest|oldest", message = "sort는 latest 또는 oldest만 허용됩니다.") String sort,
      @RequestParam(required = false) @Positive(message = "tagId는 양수여야 합니다.") Long tagId) {
    return ResponseEntity.ok(resultReportService.getHistory(memberUuid, page, size, sort, tagId));
  }

  @Operation(summary = "결과 리포트 상세 조회", description = "결과 리포트 ID로 퀴즈 풀이 결과를 조회합니다.")
  @GetMapping("/{reportId}")
  public ResponseEntity<QuizAttemptResultResponse> getResultReport(
      @PathVariable("reportId") Long reportId, @AuthenticationPrincipal String memberUuid) {
    QuizAttemptResultResponse response = quizSolveService.getResultReport(memberUuid, reportId);
    return ResponseEntity.ok(response);
  }
}
