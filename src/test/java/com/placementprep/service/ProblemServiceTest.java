package com.placementprep.service;

import static com.placementprep.service.TestFixtures.CLOCK;
import static com.placementprep.service.TestFixtures.TODAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.placementprep.core.Sm2;
import com.placementprep.domain.Difficulty;
import com.placementprep.domain.Platform;
import com.placementprep.domain.Problem;
import com.placementprep.domain.Review;
import com.placementprep.domain.Topic;
import com.placementprep.domain.User;
import com.placementprep.repository.ProblemRepository;
import com.placementprep.repository.ReviewRepository;
import com.placementprep.repository.UserRepository;
import com.placementprep.web.dto.ProblemRequest;
import com.placementprep.web.dto.ProblemView;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;

class ProblemServiceTest {

    private ProblemRepository problems;
    private ReviewRepository reviews;
    private ProblemService service;
    private final User user = TestFixtures.user(1L);

    @BeforeEach
    void setUp() {
        problems = mock(ProblemRepository.class);
        reviews = mock(ReviewRepository.class);
        UserRepository users = mock(UserRepository.class);
        service = new ProblemService(problems, reviews, users, CLOCK);
        when(users.findById(1L)).thenReturn(Optional.of(user));
        when(problems.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private Problem existing(Sm2.State state, java.time.LocalDate next) {
        Problem p = new Problem(user, "Coin Change", Platform.LEETCODE, Difficulty.MEDIUM, Topic.DP, TODAY.minusDays(3));
        p.setId(7L);
        p.applySchedule(state, next, null);
        when(problems.findByIdAndUserId(7L, 1L)).thenReturn(Optional.of(p));
        return p;
    }

    @Test
    void newProblemIsDueForRevisionTheNextDay() {
        ProblemView view = service.create(1L, new ProblemRequest(" Two Sum ", "", Platform.LEETCODE, Difficulty.EASY,
                Topic.HASHING, null, null));
        assertThat(view.title()).isEqualTo("Two Sum");
        assertThat(view.url()).isNull();
        assertThat(view.solvedOn()).isEqualTo(TODAY);
        assertThat(view.repetitions()).isEqualTo(1);
        assertThat(view.intervalDays()).isEqualTo(1);
        assertThat(view.ease()).isEqualTo(2.5);
        assertThat(view.nextReviewOn()).isEqualTo(TODAY.plusDays(1));
        assertThat(view.dueInDays()).isEqualTo(1L);
    }

    @Test
    void backdatedProblemCanBeOverdueStraightAway() {
        ProblemView view = service.create(1L, new ProblemRequest("LRU Cache", null, Platform.LEETCODE,
                Difficulty.MEDIUM, Topic.LINKED_LIST, TODAY.minusDays(5), "Use a doubly linked list"));
        assertThat(view.nextReviewOn()).isEqualTo(TODAY.minusDays(4));
        assertThat(view.dueInDays()).isEqualTo(-4L);
    }

    @Test
    void solvedDateCannotBeInTheFuture() {
        assertThatThrownBy(() -> service.create(1L, new ProblemRequest("x", null, Platform.GFG, Difficulty.EASY,
                Topic.ARRAYS, TODAY.plusDays(1), null)))
                .isInstanceOf(ApiException.class).hasMessageContaining("future");
        verify(problems, never()).save(any());
    }

    @Test
    void goodRecallSchedulesTheNextRevisionWithSm2() {
        existing(new Sm2.State(1, 1, 250), TODAY);

        ProblemView view = service.review(1L, 7L, 4);

        assertThat(view.repetitions()).isEqualTo(2);
        assertThat(view.intervalDays()).isEqualTo(6);
        assertThat(view.nextReviewOn()).isEqualTo(TODAY.plusDays(6));
        assertThat(view.lastQuality()).isEqualTo(4);

        ArgumentCaptor<Review> review = ArgumentCaptor.forClass(Review.class);
        verify(reviews).save(review.capture());
        assertThat(review.getValue().getQuality()).isEqualTo(4);
        assertThat(review.getValue().getReviewedOn()).isEqualTo(TODAY);
        assertThat(review.getValue().getIntervalAfter()).isEqualTo(6);
    }

    @Test
    void forgottenProblemComesBackTomorrow() {
        existing(new Sm2.State(4, 30, 250), TODAY.minusDays(2));
        ProblemView view = service.review(1L, 7L, 1);
        assertThat(view.repetitions()).isZero();
        assertThat(view.nextReviewOn()).isEqualTo(TODAY.plusDays(1));
        assertThat(view.ease()).isEqualTo(1.96);
    }

    @Test
    void invalidGradeIsRejectedBeforeTouchingTheDatabase() {
        assertThatThrownBy(() -> service.review(1L, 7L, 7))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(problems, never()).findByIdAndUserId(any(), any());
    }

    @Test
    void reviewingSomeoneElsesProblemIsNotFound() {
        assertThatThrownBy(() -> service.review(1L, 404L, 4)).isInstanceOf(ApiException.class)
                .hasMessage("Problem not found");
    }

    @Test
    void blankSearchIsTreatedAsNoFilter() {
        when(problems.search(1L, null, null, null)).thenReturn(List.of());
        assertThat(service.list(1L, null, null, "   ")).isEmpty();
        verify(problems).search(1L, null, null, null);
        service.list(1L, Topic.DP, Difficulty.HARD, " knap ");
        verify(problems).search(1L, Topic.DP, Difficulty.HARD, "knap");
    }

    @Test
    void deleteRemovesReviewsFirst() {
        Problem p = existing(new Sm2.State(1, 1, 250), TODAY);
        service.delete(1L, 7L);
        verify(reviews).deleteByProblemId(7L);
        verify(problems).delete(p);
    }
}
