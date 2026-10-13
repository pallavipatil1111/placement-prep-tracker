package com.placementprep.repository;

import java.time.LocalDate;

/** Number of practice events on one day (JPQL constructor projection). */
public record DayCount(LocalDate day, Long count) {
}
