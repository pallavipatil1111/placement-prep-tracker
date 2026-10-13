package com.placementprep.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Conversion funnel: how many applications reached each pipeline stage, and what share of the
 * previous stage converted. An application counts as having reached a stage if its furthest
 * stage ever is at or beyond it - a candidate rejected after the interview still counts as
 * applied, cleared the online test and interviewed.
 */
public final class Funnel {

    public record Step(Stage stage, long count, Double conversionPercent) {
    }

    private Funnel() {
    }

    /**
     * @param stageHistories for each application, every stage it has ever been in (in any order)
     */
    public static List<Step> of(Collection<? extends Collection<Stage>> stageHistories) {
        long[] counts = new long[Stage.FUNNEL.size()];
        for (Collection<Stage> history : stageHistories) {
            Stage furthest = furthest(history);
            if (furthest == null) {
                continue;
            }
            for (int i = 0; i < Stage.FUNNEL.size(); i++) {
                if (furthest.atLeast(Stage.FUNNEL.get(i))) {
                    counts[i]++;
                }
            }
        }
        List<Step> steps = new ArrayList<>();
        for (int i = 0; i < counts.length; i++) {
            Double conversion = null;
            if (i > 0 && counts[i - 1] > 0) {
                conversion = Math.round(counts[i] * 1000.0 / counts[i - 1]) / 10.0;
            }
            steps.add(new Step(Stage.FUNNEL.get(i), counts[i], conversion));
        }
        return steps;
    }

    static Stage furthest(Collection<Stage> history) {
        Stage best = null;
        for (Stage s : history) {
            if (s != null && s.isPipeline() && (best == null || s.ordinal() > best.ordinal())) {
                best = s;
            }
        }
        return best;
    }
}
