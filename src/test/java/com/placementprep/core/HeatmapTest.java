package com.placementprep.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class HeatmapTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 14); // a Wednesday

    @Test
    void buildsMondayAlignedWeeksEndingThisWeek() {
        Heatmap.Grid grid = Heatmap.build(Map.of(), TODAY, 2);
        assertThat(grid.weeks()).hasSize(2);
        assertThat(grid.weeks()).allMatch(w -> w.size() == 7);
        assertThat(grid.weeks().get(0).get(0).date()).isEqualTo(LocalDate.of(2026, 10, 5));
        assertThat(grid.weeks().get(0).get(0).date().getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(grid.weeks().get(1).get(6).date()).isEqualTo(LocalDate.of(2026, 10, 18));
    }

    @Test
    void shadesRelativeToTheBusiestDayAndBlanksTheFuture() {
        Map<LocalDate, Integer> counts = Map.of(
                LocalDate.of(2026, 10, 5), 1,
                LocalDate.of(2026, 10, 12), 4,
                LocalDate.of(2026, 10, 13), 2,
                LocalDate.of(2026, 9, 1), 50); // outside the window: ignored
        Heatmap.Grid grid = Heatmap.build(counts, TODAY, 2);

        assertThat(grid.max()).isEqualTo(4);
        assertThat(grid.activeDays()).isEqualTo(3);
        assertThat(grid.total()).isEqualTo(7);

        List<Heatmap.Cell> thisWeek = grid.weeks().get(1);
        assertThat(thisWeek.get(0)).isEqualTo(new Heatmap.Cell(LocalDate.of(2026, 10, 12), 4, 4));
        assertThat(thisWeek.get(1)).isEqualTo(new Heatmap.Cell(LocalDate.of(2026, 10, 13), 2, 2));
        assertThat(thisWeek.get(2)).isEqualTo(new Heatmap.Cell(TODAY, 0, 0));
        assertThat(thisWeek.get(3).count()).isNull();
        assertThat(grid.weeks().get(0).get(0).level()).isEqualTo(1);
    }

    @Test
    void levelScalesFromOneToFour() {
        assertThat(Heatmap.level(0, 10)).isZero();
        assertThat(Heatmap.level(1, 10)).isEqualTo(1);
        assertThat(Heatmap.level(3, 10)).isEqualTo(2);
        assertThat(Heatmap.level(6, 10)).isEqualTo(3);
        assertThat(Heatmap.level(10, 10)).isEqualTo(4);
        assertThat(Heatmap.level(5, 0)).isZero();
    }

    @Test
    void rejectsEmptyWindow() {
        assertThatThrownBy(() -> Heatmap.build(Map.of(), TODAY, 0)).isInstanceOf(IllegalArgumentException.class);
    }
}
