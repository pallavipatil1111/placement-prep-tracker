package com.placementprep.web.dto;

import com.placementprep.domain.Difficulty;
import com.placementprep.domain.Platform;
import com.placementprep.domain.Topic;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record ProblemRequest(
        @NotBlank @Size(max = 150) String title,
        @Size(max = 500) @Pattern(regexp = "^$|^https?://.+", message = "must start with http:// or https://")
        String url,
        @NotNull Platform platform,
        @NotNull Difficulty difficulty,
        @NotNull Topic topic,
        LocalDate solvedOn,
        @Size(max = 1000) String notes) {
}
