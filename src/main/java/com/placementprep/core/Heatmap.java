package com.placementprep.core;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A GitHub-style activity calendar: one column per week (Monday to Sunday), one cell per day,
 * shaded on five levels relative to the busiest day in the window.
 */
public final class Heatmap {

    /** {@code count} is null for days after today (rendered as empty). */
    public record Cell(LocalDate date, Integer count, int level) {
    }

    public record Grid(List<List<Cell>> weeks, int max, int activeDays, int total) {
    }

    private Heatmap() {
    }

    public static Grid build(Map<LocalDate, Integer> counts, LocalDate today, int weeks) {
        if (weeks < 1) {
            throw new IllegalArgumentException("weeks must be positive");
        }
        LocalDate thisMonday = today.with(DayOfWeek.MONDAY);
        LocalDate start = thisMonday.minusWeeks(weeks - 1L);

        int max = 0;
        int active = 0;
        int total = 0;
        for (LocalDate d = start; !d.isAfter(today); d = d.plusDays(1)) {
            int c = counts.getOrDefault(d, 0);
            max = Math.max(max, c);
            if (c > 0) {
                active++;
                total += c;
            }
        }

        List<List<Cell>> columns = new ArrayList<>();
        LocalDate day = start;
        for (int w = 0; w < weeks; w++) {
            List<Cell> column = new ArrayList<>(7);
            for (int i = 0; i < 7; i++) {
                if (day.isAfter(today)) {
                    column.add(new Cell(day, null, 0));
                } else {
                    int c = counts.getOrDefault(day, 0);
                    column.add(new Cell(day, c, level(c, max)));
                }
                day = day.plusDays(1);
            }
            columns.add(column);
        }
        return new Grid(columns, max, active, total);
    }

    /** 0 for no activity, otherwise 1-4 in proportion to the busiest day. */
    static int level(int count, int max) {
        if (count <= 0 || max <= 0) {
            return 0;
        }
        return Math.min(4, (int) Math.ceil(count * 4.0 / max));
    }
}
