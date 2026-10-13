package com.placementprep.repository;

import com.placementprep.domain.Difficulty;

/** Count per difficulty from a JPQL group-by (constructor projection). */
public record DifficultyCount(Difficulty key, Long count) {
}
