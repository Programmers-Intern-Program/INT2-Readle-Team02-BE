package com.realdev.readle.domain.quiz.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import com.realdev.readle.domain.quiz.dto.request.ResultReportCursor;
import com.realdev.readle.domain.quiz.dto.request.ResultReportSort;
import com.realdev.readle.domain.quiz.dto.response.ResultReportHistoryResponse;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ResultReportQueryRepositoryTest {

  private static final String MEMBER_UUID = "11111111-1111-1111-1111-111111111111";
  private static final String OTHER_MEMBER_UUID = "22222222-2222-2222-2222-222222222222";
  private static final LocalDateTime EARLY = LocalDateTime.of(2026, 7, 18, 10, 0);
  private static final LocalDateTime LATE = LocalDateTime.of(2026, 7, 20, 11, 30);

  @Autowired private ResultReportQueryRepository resultReportQueryRepository;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private EntityManagerFactory entityManagerFactory;

  @BeforeEach
  void setUp() {
    insertMember(1L, MEMBER_UUID, "member-1");
    insertMember(2L, OTHER_MEMBER_UUID, "member-2");

    insertLearningRecord(101L, 201L, 301L, 401L, 701L, 1L, "HTTP 기초", 1, 2, EARLY, "SUBMITTED");
    insertLearningRecord(102L, 202L, 302L, 402L, 702L, 1L, "Spring 트랜잭션", 4, 5, LATE, "SUBMITTED");
    insertLearningRecord(104L, 204L, 304L, 404L, 704L, 1L, "JPA 최적화", 3, 3, LATE, "SUBMITTED");
    insertLearningRecord(103L, 203L, 303L, 403L, 703L, 2L, "다른 사용자의 학습", 5, 5, LATE, "SUBMITTED");
    insertLearningRecord(
        105L, 205L, 305L, 405L, 705L, 1L, "채점 중인 학습", 2, 3, LATE.plusHours(1), "GRADING");

    insertTag(801L, "http");
    insertTag(802L, "spring");
    insertTag(803L, "jpa");
    insertContentTag(901L, 101L, 801L);
    insertContentTag(902L, 101L, 802L);
    insertContentTag(903L, 102L, 802L);
    insertContentTag(904L, 104L, 802L);
    insertContentTag(905L, 104L, 803L);
  }

  @Test
  @DisplayName("현재 사용자의 제출 완료 결과를 최신순과 결과 ID 역순으로 조회한다")
  void findHistory_ReturnsCurrentMemberSubmittedResultsInStableLatestOrder() {
    ResultReportHistoryResponse response =
        resultReportQueryRepository.findHistory(
            MEMBER_UUID, null, 10, ResultReportSort.LATEST, null);

    assertThat(response.getContent())
        .extracting(ResultReportHistoryResponse.HistoryItem::getReportId)
        .containsExactly(704L, 702L, 701L);
    assertThat(response.getHasNext()).isFalse();
    assertThat(response.getNextCursor()).isNull();

    ResultReportHistoryResponse.HistoryItem first = response.getContent().get(0);
    assertThat(first.getQuizSetId()).isEqualTo(304L);
    assertThat(first.getTitle()).isEqualTo("JPA 최적화");
    assertThat(first.getAccuracyRate()).isEqualByComparingTo("100.00");
    assertThat(first.getCorrectCount()).isEqualTo(3);
    assertThat(first.getTotalCount()).isEqualTo(3);
    assertThat(first.getSolveDurationSeconds()).isEqualTo(600);
    assertThat(first.getCompletedAt()).isEqualTo(LATE);
    assertThat(first.getTags())
        .extracting(
            ResultReportHistoryResponse.TagInfo::getTagId,
            ResultReportHistoryResponse.TagInfo::getName)
        .containsExactly(tuple(803L, "jpa"), tuple(802L, "spring"));
  }

  @Test
  @DisplayName("오래된순 커서 조회에서도 결과 ID 보조 정렬을 유지한다")
  void findHistory_AppliesOldestOrderAndCursor() {
    ResultReportHistoryResponse firstPage =
        resultReportQueryRepository.findHistory(
            MEMBER_UUID, null, 2, ResultReportSort.OLDEST, null);
    ResultReportCursor cursor =
        ResultReportCursor.decode(firstPage.getNextCursor(), ResultReportSort.OLDEST);
    ResultReportHistoryResponse secondPage =
        resultReportQueryRepository.findHistory(
            MEMBER_UUID, cursor, 2, ResultReportSort.OLDEST, null);

    assertThat(firstPage.getContent())
        .extracting(ResultReportHistoryResponse.HistoryItem::getReportId)
        .containsExactly(701L, 702L);
    assertThat(firstPage.getHasNext()).isTrue();
    assertThat(firstPage.getNextCursor()).isNotBlank();
    assertThat(secondPage.getContent())
        .extracting(ResultReportHistoryResponse.HistoryItem::getReportId)
        .containsExactly(704L);
    assertThat(secondPage.getHasNext()).isFalse();
    assertThat(secondPage.getNextCursor()).isNull();
  }

  @Test
  @DisplayName("최신순 커서는 동일 완료 시각의 결과를 중복하거나 누락하지 않는다")
  void findHistory_AppliesLatestCursorWithStableIdBoundary() {
    ResultReportHistoryResponse firstPage =
        resultReportQueryRepository.findHistory(
            MEMBER_UUID, null, 2, ResultReportSort.LATEST, null);
    ResultReportCursor cursor =
        ResultReportCursor.decode(firstPage.getNextCursor(), ResultReportSort.LATEST);
    ResultReportHistoryResponse secondPage =
        resultReportQueryRepository.findHistory(
            MEMBER_UUID, cursor, 2, ResultReportSort.LATEST, null);

    assertThat(firstPage.getContent())
        .extracting(ResultReportHistoryResponse.HistoryItem::getReportId)
        .containsExactly(704L, 702L);
    assertThat(secondPage.getContent())
        .extracting(ResultReportHistoryResponse.HistoryItem::getReportId)
        .containsExactly(701L);
  }

  @Test
  @DisplayName("태그가 지정되면 해당 태그의 학습 결과만 조회하고 전체 건수도 동일 조건으로 계산한다")
  void findHistory_FiltersByTag() {
    ResultReportHistoryResponse response =
        resultReportQueryRepository.findHistory(
            MEMBER_UUID, null, 10, ResultReportSort.LATEST, 801L);

    assertThat(response.getContent())
        .extracting(ResultReportHistoryResponse.HistoryItem::getReportId)
        .containsExactly(701L);
    assertThat(response.getHasNext()).isFalse();
  }

  @Test
  @DisplayName("학습 이력이 없는 사용자는 빈 페이지를 반환한다")
  void findHistory_ReturnsEmptyPage() {
    ResultReportHistoryResponse response =
        resultReportQueryRepository.findHistory(
            "unknown-member", null, 10, ResultReportSort.LATEST, null);

    assertThat(response.getContent()).isEmpty();
    assertThat(response.getHasNext()).isFalse();
    assertThat(response.getNextCursor()).isNull();
  }

  @Test
  @DisplayName("페이지 크기와 관계없이 태그를 일괄 조회해 N+1을 방지한다")
  void findHistory_FetchesTagsWithoutNPlusOne() {
    Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    statistics.setStatisticsEnabled(true);
    statistics.clear();

    resultReportQueryRepository.findHistory(MEMBER_UUID, null, 10, ResultReportSort.LATEST, null);

    assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
  }

  private void insertMember(Long id, String uuid, String oauthId) {
    jdbcTemplate.update(
        """
        INSERT INTO member
          (id, uuid, oauth_provider, oauth_id, nickname, created_at, updated_at, last_login_at)
        VALUES (?, ?, 'GOOGLE', ?, ?, ?, ?, ?)
        """,
        id,
        uuid,
        oauthId,
        oauthId,
        LocalDateTime.of(2026, 7, 1, 0, 0),
        LocalDateTime.of(2026, 7, 1, 0, 0),
        LocalDateTime.of(2026, 7, 1, 0, 0));
  }

  private void insertLearningRecord(
      Long contentId,
      Long validationId,
      Long quizSetId,
      Long attemptId,
      Long resultId,
      Long memberId,
      String title,
      int correctCount,
      int totalCount,
      LocalDateTime completedAt,
      String attemptStatus) {
    jdbcTemplate.update(
        """
        INSERT INTO content
          (id, member_id, title, input_type, raw_text, crawl_status, created_at, updated_at)
        VALUES (?, ?, ?, 'TEXT', '테스트 본문', 'NOT_APPLICABLE', ?, ?)
        """,
        contentId,
        memberId,
        title,
        completedAt.minusHours(2),
        completedAt.minusHours(2));
    jdbcTemplate.update(
        """
        INSERT INTO content_validation
          (id, content_id, validation_method, status, validation_score, created_at, validated_at)
        VALUES (?, ?, 'AI', 'PASSED', 90.00, ?, ?)
        """,
        validationId,
        contentId,
        completedAt.minusHours(2),
        completedAt.minusHours(1));
    jdbcTemplate.update(
        """
        INSERT INTO quiz_set
          (id, content_id, status, question_count, created_at, completed_at,
           is_bypassed, source_validation_id)
        VALUES (?, ?, 'COMPLETED', ?, ?, ?, FALSE, ?)
        """,
        quizSetId,
        contentId,
        totalCount,
        completedAt.minusHours(1),
        completedAt.minusMinutes(50),
        validationId);
    jdbcTemplate.update(
        """
        INSERT INTO quiz_attempt
          (id, quiz_set_id, member_id, status, started_at, submitted_at)
        VALUES (?, ?, ?, ?, ?, ?)
        """,
        attemptId,
        quizSetId,
        memberId,
        attemptStatus,
        completedAt.minusMinutes(10),
        "SUBMITTED".equals(attemptStatus) ? completedAt : null);
    jdbcTemplate.update(
        """
        INSERT INTO quiz_result
          (id, attempt_id, accuracy_rate, correct_count, total_count,
           solve_duration_seconds, completed_at)
        VALUES (?, ?, ?, ?, ?, 600, ?)
        """,
        resultId,
        attemptId,
        BigDecimal.valueOf(correctCount)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(totalCount), 2, RoundingMode.HALF_UP),
        correctCount,
        totalCount,
        completedAt);
  }

  private void insertTag(Long id, String name) {
    jdbcTemplate.update(
        "INSERT INTO tag (id, name, created_at) VALUES (?, ?, ?)",
        id,
        name,
        LocalDateTime.of(2026, 7, 1, 0, 0));
  }

  private void insertContentTag(Long id, Long contentId, Long tagId) {
    jdbcTemplate.update(
        "INSERT INTO content_tag (id, content_id, tag_id, created_at) VALUES (?, ?, ?, ?)",
        id,
        contentId,
        tagId,
        LocalDateTime.of(2026, 7, 1, 0, 0));
  }
}
