package com.placementprep.web.dto;

import com.placementprep.core.Stage;
import jakarta.validation.constraints.NotNull;

public record StageRequest(@NotNull Stage stage) {
}
