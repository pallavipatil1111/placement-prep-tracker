package com.placementprep.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class FunnelTest {

    @Test
    void countsEveryStageAnApplicationGotThrough() {
        List<List<Stage>> histories = List.of(
                List.of(Stage.APPLIED, Stage.OA, Stage.INTERVIEW, Stage.OFFER),
                List.of(Stage.APPLIED, Stage.OA, Stage.REJECTED),
                List.of(Stage.APPLIED),
                List.of(Stage.WISHLIST),
                List.of(Stage.APPLIED, Stage.INTERVIEW, Stage.REJECTED)); // no online test round

        List<Funnel.Step> steps = Funnel.of(histories);

        assertThat(steps).extracting(Funnel.Step::stage)
                .containsExactly(Stage.APPLIED, Stage.OA, Stage.INTERVIEW, Stage.OFFER);
        assertThat(steps).extracting(Funnel.Step::count).containsExactly(4L, 3L, 2L, 1L);
        assertThat(steps.get(0).conversionPercent()).isNull();
        assertThat(steps.get(1).conversionPercent()).isEqualTo(75.0);
        assertThat(steps.get(2).conversionPercent()).isEqualTo(66.7);
        assertThat(steps.get(3).conversionPercent()).isEqualTo(50.0);
    }

    @Test
    void orderOfHistoryDoesNotMatter() {
        List<Funnel.Step> steps = Funnel.of(List.of(List.of(Stage.REJECTED, Stage.INTERVIEW, Stage.APPLIED)));
        assertThat(steps).extracting(Funnel.Step::count).containsExactly(1L, 1L, 1L, 0L);
    }

    @Test
    void emptyBoardGivesZerosWithoutDividingByZero() {
        List<Funnel.Step> steps = Funnel.of(List.of());
        assertThat(steps).extracting(Funnel.Step::count).containsExactly(0L, 0L, 0L, 0L);
        assertThat(steps).allMatch(s -> s.conversionPercent() == null);
    }

    @Test
    void furthestIgnoresRejectedAndWishlistOnlyHistories() {
        assertThat(Funnel.furthest(List.of(Stage.REJECTED))).isNull();
        assertThat(Funnel.furthest(List.of(Stage.WISHLIST, Stage.REJECTED))).isEqualTo(Stage.WISHLIST);
    }
}
