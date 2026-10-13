package com.placementprep.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class Sm2Test {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 14);

    @Test
    void firstSuccessfulReviewComesBackTomorrow() {
        Sm2.Result r = Sm2.review(Sm2.State.fresh(), 4, TODAY);
        assertThat(r.state()).isEqualTo(new Sm2.State(1, 1, 250));
        assertThat(r.nextReview()).isEqualTo(TODAY.plusDays(1));
    }

    @Test
    void secondSuccessfulReviewWaitsSixDays() {
        Sm2.Result r = Sm2.review(new Sm2.State(1, 1, 250), 4, TODAY);
        assertThat(r.state()).isEqualTo(new Sm2.State(2, 6, 250));
        assertThat(r.nextReview()).isEqualTo(TODAY.plusDays(6));
    }

    @Test
    void laterIntervalsGrowByTheUpdatedEaseFactor() {
        Sm2.Result easy = Sm2.review(new Sm2.State(2, 6, 250), 5, TODAY);
        assertThat(easy.state()).isEqualTo(new Sm2.State(3, 16, 260)); // ease +0.10, 6 x 2.6 = 15.6

        Sm2.Result good = Sm2.review(new Sm2.State(2, 6, 250), 4, TODAY);
        assertThat(good.state()).isEqualTo(new Sm2.State(3, 15, 250)); // 6 x 2.5

        Sm2.Result hard = Sm2.review(new Sm2.State(2, 6, 250), 3, TODAY);
        assertThat(hard.state()).isEqualTo(new Sm2.State(3, 14, 236)); // ease -0.14, 6 x 2.36 = 14.2
    }

    @Test
    void forgettingResetsRepetitionsAndLowersEase() {
        Sm2.Result r = Sm2.review(new Sm2.State(4, 40, 250), 2, TODAY);
        assertThat(r.state()).isEqualTo(new Sm2.State(0, 1, 218));
        assertThat(r.nextReview()).isEqualTo(TODAY.plusDays(1));
    }

    @Test
    void easeNeverDropsBelowOnePointThree() {
        Sm2.Result r = Sm2.review(new Sm2.State(5, 10, 135), 0, TODAY);
        assertThat(r.state().easeX100()).isEqualTo(Sm2.MIN_EASE);
    }

    @Test
    void intervalIsCappedAtOneYear() {
        Sm2.Result r = Sm2.review(new Sm2.State(10, 300, 250), 5, TODAY);
        assertThat(r.state().intervalDays()).isEqualTo(Sm2.MAX_INTERVAL_DAYS);
    }

    @Test
    void easeChangesMatchTheSm2FormulaForEveryGrade() {
        int[] expectedDelta = {-80, -54, -32, -14, 0, 10};
        for (int q = 0; q <= 5; q++) {
            Sm2.Result r = Sm2.review(new Sm2.State(3, 10, 250), q, TODAY);
            assertThat(r.state().easeX100()).as("grade %d", q).isEqualTo(250 + expectedDelta[q]);
        }
    }

    @Test
    void rejectsGradesOutsideZeroToFive() {
        assertThatThrownBy(() -> Sm2.review(Sm2.State.fresh(), -1, TODAY)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Sm2.review(Sm2.State.fresh(), 6, TODAY)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void steadyGoodRecallSpacesReviewsFurtherApart() {
        Sm2.State state = new Sm2.State(1, 1, 250);
        int previous = 0;
        for (int i = 0; i < 6; i++) {
            state = Sm2.review(state, 4, TODAY).state();
            assertThat(state.intervalDays()).isGreaterThan(previous);
            previous = state.intervalDays();
        }
        assertThat(previous).isGreaterThan(100);
    }
}
