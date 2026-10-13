package com.placementprep.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class StreaksTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 14);

    private static LocalDate ago(int days) {
        return TODAY.minusDays(days);
    }

    @Test
    void noActivityMeansNoStreak() {
        assertThat(Streaks.of(List.of(), TODAY)).isEqualTo(new Streaks.Summary(0, 0, null));
    }

    @Test
    void countsConsecutiveDaysEndingToday() {
        Streaks.Summary s = Streaks.of(List.of(ago(0), ago(1), ago(2)), TODAY);
        assertThat(s.current()).isEqualTo(3);
        assertThat(s.longest()).isEqualTo(3);
        assertThat(s.lastActive()).isEqualTo(TODAY);
    }

    @Test
    void streakSurvivesUntilTodayIsOver() {
        Streaks.Summary s = Streaks.of(List.of(ago(1), ago(2)), TODAY);
        assertThat(s.current()).isEqualTo(2);
    }

    @Test
    void missingYesterdayAndTodayBreaksTheStreak() {
        Streaks.Summary s = Streaks.of(List.of(ago(2), ago(3), ago(4), ago(5)), TODAY);
        assertThat(s.current()).isZero();
        assertThat(s.longest()).isEqualTo(4);
    }

    @Test
    void longestIsTrackedAcrossGaps() {
        Streaks.Summary s = Streaks.of(List.of(ago(9), ago(8), ago(7), ago(6), ago(4), ago(1), ago(0)), TODAY);
        assertThat(s.current()).isEqualTo(2);
        assertThat(s.longest()).isEqualTo(4);
    }

    @Test
    void duplicatesAndFutureDatesAreIgnored() {
        Streaks.Summary s = Streaks.of(List.of(ago(0), ago(0), ago(1), TODAY.plusDays(1)), TODAY);
        assertThat(s.current()).isEqualTo(2);
        assertThat(s.longest()).isEqualTo(2);
        assertThat(s.lastActive()).isEqualTo(TODAY);
    }

    @Test
    void streakCrossesMonthAndYearBoundaries() {
        LocalDate newYear = LocalDate.of(2027, 1, 1);
        Streaks.Summary s = Streaks.of(List.of(LocalDate.of(2026, 12, 30), LocalDate.of(2026, 12, 31), newYear), newYear);
        assertThat(s.current()).isEqualTo(3);
    }
}
