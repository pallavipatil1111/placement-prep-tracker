package com.placementprep.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record GoalRequest(@Min(1) @Max(50) int dailyGoal) {
}
