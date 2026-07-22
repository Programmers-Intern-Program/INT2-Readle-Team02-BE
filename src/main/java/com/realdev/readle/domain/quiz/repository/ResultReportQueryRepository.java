package com.realdev.readle.domain.quiz.repository;

import static com.realdev.readle.domain.content.entity.QContent.content;
import static com.realdev.readle.domain.quiz.entity.QQuizAttempt.quizAttempt;
import static com.realdev.readle.domain.quiz.entity.QQuizResult.quizResult;
import static com.realdev.readle.domain.quiz.entity.QQuizSet.quizSet;
import static com.realdev.readle.domain.tag.entity.QContentTag.contentTag;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.realdev.readle.domain.quiz.dto.request.ResultReportCursor;
import com.realdev.readle.domain.quiz.dto.request.ResultReportSort;
import com.realdev.readle.domain.quiz.dto.response.ResultReportHistoryResponse;
import com.realdev.readle.domain.quiz.entity.AttemptStatus;
import com.realdev.readle.domain.quiz.entity.QuizAttempt;
import com.realdev.readle.domain.quiz.entity.QuizResult;
import com.realdev.readle.domain.tag.repository.ContentTagRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ResultReportQueryRepository {

  private final JPAQueryFactory queryFactory;
  private final ContentTagRepository contentTagRepository;

  public ResultReportHistoryResponse findHistory(
      String memberUuid, ResultReportCursor cursor, int size, ResultReportSort sort, Long tagId) {
    BooleanBuilder conditions = historyConditions(memberUuid, cursor, sort, tagId);
    OrderSpecifier<?> completedAtOrder =
        sort == ResultReportSort.LATEST
            ? quizResult.completedAt.desc()
            : quizResult.completedAt.asc();
    OrderSpecifier<?> idOrder =
        sort == ResultReportSort.LATEST ? quizResult.id.desc() : quizResult.id.asc();

    List<QuizResult> fetchedResults =
        queryFactory
            .selectFrom(quizResult)
            .join(quizResult.quizAttempt, quizAttempt)
            .fetchJoin()
            .join(quizAttempt.quizSet, quizSet)
            .fetchJoin()
            .join(quizSet.content, content)
            .fetchJoin()
            .where(conditions)
            .orderBy(completedAtOrder, idOrder)
            .limit(size + 1L)
            .fetch();

    boolean hasNext = fetchedResults.size() > size;
    List<QuizResult> results = hasNext ? fetchedResults.subList(0, size) : fetchedResults;

    Map<Long, List<ResultReportHistoryResponse.TagInfo>> tagsByContentId =
        fetchTagsByContentId(
            results.stream()
                .map(result -> result.getQuizAttempt().getQuizSet().getContent().getId())
                .distinct()
                .toList());

    List<ResultReportHistoryResponse.HistoryItem> items =
        results.stream().map(result -> toHistoryItem(result, tagsByContentId)).toList();
    String nextCursor =
        hasNext
            ? new ResultReportCursor(
                    sort,
                    results.get(results.size() - 1).getCompletedAt(),
                    results.get(results.size() - 1).getId())
                .encode()
            : null;

    return ResultReportHistoryResponse.of(items, size, nextCursor, hasNext);
  }

  private BooleanBuilder historyConditions(
      String memberUuid, ResultReportCursor cursor, ResultReportSort sort, Long tagId) {
    BooleanBuilder conditions =
        new BooleanBuilder()
            .and(quizAttempt.member.uuid.eq(memberUuid))
            .and(quizAttempt.status.eq(AttemptStatus.SUBMITTED));

    if (cursor != null) {
      conditions.and(
          sort == ResultReportSort.LATEST
              ? quizResult
                  .completedAt
                  .lt(cursor.completedAt())
                  .or(
                      quizResult
                          .completedAt
                          .eq(cursor.completedAt())
                          .and(quizResult.id.lt(cursor.reportId())))
              : quizResult
                  .completedAt
                  .gt(cursor.completedAt())
                  .or(
                      quizResult
                          .completedAt
                          .eq(cursor.completedAt())
                          .and(quizResult.id.gt(cursor.reportId()))));
    }

    if (tagId != null) {
      conditions.and(
          JPAExpressions.selectOne()
              .from(contentTag)
              .where(contentTag.content.id.eq(content.id), contentTag.tag.id.eq(tagId))
              .exists());
    }
    return conditions;
  }

  private ResultReportHistoryResponse.HistoryItem toHistoryItem(
      QuizResult result, Map<Long, List<ResultReportHistoryResponse.TagInfo>> tagsByContentId) {
    QuizAttempt attempt = result.getQuizAttempt();
    Long contentId = attempt.getQuizSet().getContent().getId();

    return ResultReportHistoryResponse.HistoryItem.builder()
        .reportId(result.getId())
        .quizSetId(attempt.getQuizSet().getId())
        .title(attempt.getQuizSet().getContent().getTitle())
        .accuracyRate(result.getAccuracyRate())
        .correctCount(result.getCorrectCount())
        .totalCount(result.getTotalCount())
        .solveDurationSeconds(result.getSolveDurationSeconds())
        .completedAt(result.getCompletedAt())
        .tags(tagsByContentId.getOrDefault(contentId, List.of()))
        .build();
  }

  private Map<Long, List<ResultReportHistoryResponse.TagInfo>> fetchTagsByContentId(
      List<Long> contentIds) {
    if (contentIds.isEmpty()) {
      return Map.of();
    }

    Map<Long, List<ResultReportHistoryResponse.TagInfo>> tagsByContentId = new LinkedHashMap<>();
    contentTagRepository
        .findByContentIdInWithTag(contentIds)
        .forEach(
            contentTag ->
                tagsByContentId
                    .computeIfAbsent(contentTag.getContent().getId(), ignored -> new ArrayList<>())
                    .add(
                        ResultReportHistoryResponse.TagInfo.builder()
                            .tagId(contentTag.getTag().getId())
                            .name(contentTag.getTag().getName())
                            .build()));
    return tagsByContentId;
  }
}
