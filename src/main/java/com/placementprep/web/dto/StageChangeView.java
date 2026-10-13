package com.placementprep.web.dto;

import com.placementprep.core.Stage;
import com.placementprep.domain.StageChange;
import java.time.Instant;

public record StageChangeView(Stage from, Stage to, Instant changedAt) {

    public static StageChangeView of(StageChange c) {
        return new StageChangeView(c.getFromStage(), c.getToStage(), c.getChangedAt());
    }
}
