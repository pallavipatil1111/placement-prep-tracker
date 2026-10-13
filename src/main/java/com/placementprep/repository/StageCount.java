package com.placementprep.repository;

import com.placementprep.core.Stage;

/** Count per stage from a JPQL group-by (constructor projection). */
public record StageCount(Stage key, Long count) {
}
