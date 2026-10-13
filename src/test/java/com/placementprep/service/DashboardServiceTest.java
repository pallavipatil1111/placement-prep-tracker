package com.placementprep.service;

import static com.placementprep.service.TestFixtures.CLOCK;
import static com.placementprep.service.TestFixtures.TODAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.placementprep.core.Funnel;
import com.placementprep.core.Stage;
import com.placementprep.domain.Application;
import com.placementprep.domain.Difficulty;
import com.placementprep.domain.Source;
import com.placementprep.domain.Topic;
import com.placementprep.domain.User;
import com.placementprep.repository.ApplicationRepository;
import com.placementprep.repository.DayCount;
import com.placementprep.repository.DifficultyCount;
import com.placementprep.repository.ProblemRepository;
import com.placementprep.repository.ReviewRepository;
import com.placementprep.repository.StageChangeRepository;
import com.placementprep.repository.StageCount;
import com.placementprep.repository.StageVisit;
import com.placementprep.repository.TopicCount;
import com.placementprep.repository.UserRepository;
import com.placementprep.web.dto.DashboardView;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DashboardServiceTest {

    private DashboardService service;

    @BeforeEach
    void setUp() {
        ProblemRepository problems = mock(ProblemRepository.class);
        ReviewRepository reviews = mock(ReviewRepository.class);
        ApplicationRepository applications = mock(ApplicationRepository.class);
        StageChangeRepository stageChanges = mock(StageChangeRepository.class);
        UserRepository users = mock(UserRepository.class);
        User user = TestFixtures.user(1L);
        user.setDailyGoal(4);
        when(users.findById(1L)).thenReturn(Optional.of(user));

        when(problems.solvedPerDay(1L)).thenReturn(List.of(
                new DayCount(TODAY, 2L), new DayCount(TODAY.minusDays(1), 1L), new DayCount(TODAY.minusDays(2), 1L)));
        when(reviews.reviewsPerDay(1L)).thenReturn(List.of(
                new DayCount(TODAY, 1L), new DayCount(TODAY.minusDays(5), 3L), new DayCount(TODAY.minusDays(6), 1L),
                new DayCount(TODAY.minusDays(7), 1L), new DayCount(TODAY.minusDays(8), 1L),
                new DayCount(TODAY.minusDays(9), 1L)));
        when(problems.countByUserId(1L)).thenReturn(7L);
        when(problems.countByUserIdAndNextReviewOnLessThanEqual(1L, TODAY)).thenReturn(2L);
        when(problems.countByDifficulty(1L)).thenReturn(List.of(
                new DifficultyCount(Difficulty.EASY, 2L), new DifficultyCount(Difficulty.MEDIUM, 5L)));
        when(problems.countByTopic(1L)).thenReturn(List.of(new TopicCount(Topic.DP, 3L),
                new TopicCount(Topic.GRAPHS, 4L), new TopicCount(Topic.ARRAYS, 3L)));
        when(applications.countByStage(1L)).thenReturn(List.of(new StageCount(Stage.APPLIED, 3L),
                new StageCount(Stage.OA, 1L), new StageCount(Stage.OFFER, 1L), new StageCount(Stage.REJECTED, 2L)));
        when(stageChanges.findVisitsForUser(1L)).thenReturn(List.of(
                new StageVisit(1L, Stage.APPLIED), new StageVisit(1L, Stage.OA), new StageVisit(1L, Stage.INTERVIEW),
                new StageVisit(1L, Stage.OFFER), new StageVisit(2L, Stage.APPLIED), new StageVisit(2L, Stage.REJECTED),
                new StageVisit(3L, Stage.APPLIED), new StageVisit(3L, Stage.OA)));

        Application upcoming = new Application(user, "Qualys", "ASE", Source.REFERRAL, Stage.INTERVIEW);
        upcoming.setNextStepOn(TODAY.plusDays(2));
        Application rejected = new Application(user, "TCS", "Digital", Source.CAMPUS, Stage.REJECTED);
        rejected.setNextStepOn(TODAY.plusDays(1));
        when(applications.findTop5ByUserIdAndNextStepOnGreaterThanEqualOrderByNextStepOnAsc(any(), any()))
                .thenReturn(List.of(rejected, upcoming));

        service = new DashboardService(problems, reviews, applications, stageChanges, users, CLOCK);
    }

    @Test
    void practiceStatsCombineSolvesAndRevisions() {
        DashboardView d = service.build(1L, 26);
        assertThat(d.today()).isEqualTo(TODAY);
        assertThat(d.dailyGoal()).isEqualTo(4);
        assertThat(d.todayCount()).isEqualTo(3);
        assertThat(d.currentStreak()).isEqualTo(3);
        assertThat(d.longestStreak()).isEqualTo(5); // 9..5 days ago
        assertThat(d.totalSolved()).isEqualTo(7L);
        assertThat(d.dueCount()).isEqualTo(2L);
    }

    @Test
    void breakdownsFillMissingBucketsAndSortTopics() {
        DashboardView d = service.build(1L, 26);
        assertThat(d.byDifficulty()).containsEntry(Difficulty.EASY, 2L).containsEntry(Difficulty.HARD, 0L);
        assertThat(d.byTopic()).extracting(TopicCount::key).containsExactly(Topic.GRAPHS, Topic.ARRAYS, Topic.DP);
    }

    @Test
    void applicationSummaryAndFunnel() {
        DashboardView d = service.build(1L, 26);
        assertThat(d.totalApplications()).isEqualTo(7L);
        assertThat(d.activeApplications()).isEqualTo(4L);
        assertThat(d.offers()).isEqualTo(1L);
        assertThat(d.byStage()).containsEntry(Stage.WISHLIST, 0L).containsEntry(Stage.REJECTED, 2L);
        assertThat(d.funnel()).extracting(Funnel.Step::count).containsExactly(3L, 2L, 1L, 1L);
    }

    @Test
    void upcomingStepsSkipRejectedApplications() {
        DashboardView d = service.build(1L, 26);
        assertThat(d.upcoming()).extracting(a -> a.company()).containsExactly("Qualys");
    }

    @Test
    void heatmapWindowIsClamped() {
        assertThat(service.build(1L, 26).heatmap().weeks()).hasSize(26);
        assertThat(service.build(1L, 500).heatmap().weeks()).hasSize(53);
        assertThat(service.build(1L, 1).heatmap().weeks()).hasSize(4);
        assertThat(service.build(1L, 26).heatmap().max()).isEqualTo(3);
    }
}
