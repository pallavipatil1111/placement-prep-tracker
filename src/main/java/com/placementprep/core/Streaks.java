package com.placementprep.core;

import java.time.LocalDate;
import java.util.Collection;
import java.util.TreeSet;

/**
 * Practice streaks: consecutive calendar days with at least one solved or revised problem.
 * A streak is still "current" if the last active day was yesterday - today is not over yet.
 */
public final class Streaks {

    public record Summary(int current, int longest, LocalDate lastActive) {
    }

    private Streaks() {
    }

    public static Summary of(Collection<LocalDate> activeDays, LocalDate today) {
        TreeSet<LocalDate> days = new TreeSet<>();
        for (LocalDate d : activeDays) {
            if (d != null && !d.isAfter(today)) {
                days.add(d);
            }
        }
        if (days.isEmpty()) {
            return new Summary(0, 0, null);
        }

        int longest = 0;
        int run = 0;
        LocalDate previous = null;
        for (LocalDate d : days) {
            run = (previous != null && previous.plusDays(1).equals(d)) ? run + 1 : 1;
            longest = Math.max(longest, run);
            previous = d;
        }

        LocalDate last = days.last();
        int current = 0;
        if (last.equals(today) || last.equals(today.minusDays(1))) {
            LocalDate cursor = last;
            while (days.contains(cursor)) {
                current++;
                cursor = cursor.minusDays(1);
            }
        }
        return new Summary(current, longest, last);
    }
}
