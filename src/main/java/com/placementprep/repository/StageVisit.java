package com.placementprep.repository;

import com.placementprep.core.Stage;

/** One stage an application has been in, used to build the funnel. */
public record StageVisit(Long applicationId, Stage stage) {
}
