package com.realdev.readle.domain.quiz.service;

import com.realdev.readle.domain.quiz.dto.request.ResultReportCursor;
import com.realdev.readle.domain.quiz.dto.request.ResultReportSort;
import com.realdev.readle.domain.quiz.dto.response.ResultReportHistoryResponse;
import com.realdev.readle.domain.quiz.repository.ResultReportQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResultReportService {

  private final ResultReportQueryRepository resultReportQueryRepository;

  public ResultReportHistoryResponse getHistory(
      String memberUuid, String cursor, int size, String sort, Long tagId) {
    ResultReportSort resultReportSort = ResultReportSort.from(sort);
    return resultReportQueryRepository.findHistory(
        memberUuid,
        ResultReportCursor.decode(cursor, resultReportSort),
        size,
        resultReportSort,
        tagId);
  }
}
