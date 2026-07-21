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
  private final Integer page;
  private final Integer size;
  private final Long totalElements;
  private final Integer totalPages;
  private final Boolean first;
  private final Boolean last;

  public static ResultReportHistoryResponse of(
      List<HistoryItem> content, int page, int size, long totalElements) {
    int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / size);

    return ResultReportHistoryResponse.builder()
        .content(content)
        .page(page)
        .size(size)
        .totalElements(totalElements)
        .totalPages(totalPages)
        .first(page == 0)
        .last(totalPages == 0 || page >= totalPages - 1)
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
