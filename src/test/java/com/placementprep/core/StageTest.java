package com.placementprep.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StageTest {

    @Test
    void pipelineStagesCompareInOrder() {
        assertThat(Stage.INTERVIEW.atLeast(Stage.OA)).isTrue();
        assertThat(Stage.OA.atLeast(Stage.OA)).isTrue();
        assertThat(Stage.APPLIED.atLeast(Stage.OFFER)).isFalse();
    }

    @Test
    void rejectedIsNeverAtLeastAnything() {
        for (Stage s : Stage.values()) {
            assertThat(Stage.REJECTED.atLeast(s)).isFalse();
            assertThat(s.atLeast(Stage.REJECTED)).isFalse();
        }
    }

    @Test
    void wishlistIsTheOnlyUnsubmittedStage() {
        assertThat(Stage.WISHLIST.isSubmitted()).isFalse();
        assertThat(Stage.APPLIED.isSubmitted()).isTrue();
        assertThat(Stage.REJECTED.isSubmitted()).isTrue();
        assertThat(Stage.FUNNEL).doesNotContain(Stage.WISHLIST, Stage.REJECTED);
    }
}
