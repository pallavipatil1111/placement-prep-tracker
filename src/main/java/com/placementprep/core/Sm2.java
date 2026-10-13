package com.placementprep.core;

import java.time.LocalDate;

/**
 * The SuperMemo-2 spaced-repetition algorithm, used to schedule when a solved DSA problem should be
 * revised again.
 *
 * <p>After each revision the user grades their recall from 0 (blackout) to 5 (perfect):
 * <ul>
 *   <li>grade &lt; 3: the problem is relearned from scratch and comes back tomorrow;</li>
 *   <li>the ease factor moves by {@code 0.1 - (5-q)(0.08 + (5-q)0.02)} and never drops below 1.3;</li>
 *   <li>otherwise the interval grows 1 day, 6 days, then the previous interval times the
 *       updated ease factor - so "easy" pushes the next revision further out than "hard".</li>
 * </ul>
 * The ease factor is stored as an integer in hundredths (2.5 = 250) so no floating-point drift
 * builds up across hundreds of reviews.
 */
public final class Sm2 {

    public static final int INITIAL_EASE = 250;
    public static final int MIN_EASE = 130;
    public static final int MAX_INTERVAL_DAYS = 365;

    private Sm2() {
    }

    /** Scheduling state of one problem. */
    public record State(int repetitions, int intervalDays, int easeX100) {

        public static State fresh() {
            return new State(0, 0, INITIAL_EASE);
        }
    }

    /** Result of a review: the new state and the date the problem is due again. */
    public record Result(State state, LocalDate nextReview) {
    }

    public static Result review(State current, int quality, LocalDate today) {
        if (quality < 0 || quality > 5) {
            throw new IllegalArgumentException("Quality must be between 0 and 5");
        }
        int miss = 5 - quality;
        int ease = Math.max(MIN_EASE, current.easeX100() + 10 - miss * (8 + miss * 2));

        int repetitions;
        int interval;
        if (quality < 3) {
            repetitions = 0;
            interval = 1;
        } else {
            repetitions = current.repetitions() + 1;
            if (repetitions == 1) {
                interval = 1;
            } else if (repetitions == 2) {
                interval = 6;
            } else {
                interval = (int) Math.min(MAX_INTERVAL_DAYS,
                        (Math.max(1, current.intervalDays()) * (long) ease + 50) / 100);
            }
        }
        State next = new State(repetitions, interval, ease);
        return new Result(next, today.plusDays(interval));
    }
}
