package com.placementprep.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Recall grade: 0-2 forgot (relearn), 3 hard, 4 good, 5 easy. */
public record ReviewRequest(@NotNull @Min(0) @Max(5) Integer quality) {
}
