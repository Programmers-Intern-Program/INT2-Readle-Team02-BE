package com.realdev.readle.domain.quiz.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ResultReportHistoryResponse {

  private final List<HistoryItem> content;
  private final Integer size;
  private final String nextCursor;
  private final Boolean hasNext;

  public static ResultReportHistoryResponse of(
      List<HistoryItem> content, int size, String nextCursor, boolean hasNext) {
    return ResultReportHistoryResponse.builder()
        .content(content)
        .size(size)
        .nextCursor(nextCursor)
        .hasNext(hasNext)
        .build();
  }

  @Getter
  @Builder
  public static class HistoryItem {
    private final Long reportId;
    private final Long quizSetId;
    private final String title;
    private final BigDecimal accuracyRate;
    private final Integer correctCount;
    private final Integer totalCount;
    private final Integer solveDurationSeconds;
    private final LocalDateTime completedAt;
    private final List<TagInfo> tags;
  }

  @Getter
  @Builder
  public static class TagInfo {
    private final Long tagId;
    private final String name;
  }
}
