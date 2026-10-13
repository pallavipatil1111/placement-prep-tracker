package com.placementprep.web.dto;

import com.placementprep.core.Stage;
import com.placementprep.domain.Source;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ApplicationRequest(
        @NotBlank @Size(max = 100) String company,
        @NotBlank @Size(max = 100) String jobTitle,
        @Size(max = 80) String location,
        @NotNull Source source,
        Stage stage,
        @DecimalMin("0.0") @DecimalMax("999.99") BigDecimal packageLpa,
        @Size(max = 500) @Pattern(regexp = "^$|^https?://.+", message = "must start with http:// or https://")
        String jobUrl,
        LocalDate appliedOn,
        @Size(max = 120) String nextStep,
        LocalDate nextStepOn,
        @Size(max = 1000) String notes) {
}
