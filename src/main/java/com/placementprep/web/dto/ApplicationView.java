package com.placementprep.web.dto;

import com.placementprep.core.Stage;
import com.placementprep.domain.Application;
import com.placementprep.domain.Source;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ApplicationView(Long id, String company, String jobTitle, String location, Source source,
                              Stage stage, String stageLabel, BigDecimal packageLpa, String jobUrl,
                              LocalDate appliedOn, String nextStep, LocalDate nextStepOn, String notes,
                              Instant updatedAt) {

    public static ApplicationView of(Application a) {
        return new ApplicationView(a.getId(), a.getCompany(), a.getJobTitle(), a.getLocation(), a.getSource(),
                a.getStage(), a.getStage().label(), a.getPackageLpa(), a.getJobUrl(), a.getAppliedOn(),
                a.getNextStep(), a.getNextStepOn(), a.getNotes(), a.getUpdatedAt());
    }
}
