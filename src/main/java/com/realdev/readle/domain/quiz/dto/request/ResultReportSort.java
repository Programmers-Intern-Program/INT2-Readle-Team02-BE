package com.realdev.readle.domain.quiz.dto.request;

import com.realdev.readle.global.exception.CustomException;
import com.realdev.readle.global.exception.GlobalErrorCode;
import java.util.Locale;

public enum ResultReportSort {
  LATEST,
  OLDEST;

  public static ResultReportSort from(String value) {
    try {
      return ResultReportSort.valueOf(value.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException | NullPointerException exception) {
      throw new CustomException(GlobalErrorCode.INVALID_INPUT, "sort는 latest 또는 oldest만 허용됩니다.");
    }
  }
}
