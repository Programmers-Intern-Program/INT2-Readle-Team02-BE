package com.realdev.readle.domain.quiz.dto.request;

import com.realdev.readle.global.exception.CustomException;
import com.realdev.readle.global.exception.GlobalErrorCode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Base64;

public record ResultReportCursor(ResultReportSort sort, LocalDateTime completedAt, Long reportId) {

  private static final String DELIMITER = "|";

  public static ResultReportCursor decode(String encodedCursor, ResultReportSort expectedSort) {
    if (encodedCursor == null) {
      return null;
    }

    try {
      String decoded =
          new String(Base64.getUrlDecoder().decode(encodedCursor), StandardCharsets.UTF_8);
      String[] values = decoded.split("\\|", -1);
      if (values.length != 3) {
        throw invalidCursor();
      }

      ResultReportSort cursorSort = ResultReportSort.valueOf(values[0]);
      LocalDateTime completedAt = LocalDateTime.parse(values[1]);
      Long reportId = Long.valueOf(values[2]);
      if (cursorSort != expectedSort || reportId <= 0) {
        throw invalidCursor();
      }
      return new ResultReportCursor(cursorSort, completedAt, reportId);
    } catch (IllegalArgumentException | DateTimeParseException exception) {
      throw invalidCursor();
    }
  }

  public String encode() {
    String value = sort.name() + DELIMITER + completedAt + DELIMITER + reportId;
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(value.getBytes(StandardCharsets.UTF_8));
  }

  private static CustomException invalidCursor() {
    return new CustomException(GlobalErrorCode.INVALID_CURSOR);
  }
}
