package com.placementprep.web.dto;

import com.placementprep.domain.Difficulty;
import com.placementprep.domain.Platform;
import com.placementprep.domain.Problem;
import com.placementprep.domain.Topic;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public record ProblemView(Long id, String title, String url, Platform platform, Difficulty difficulty, Topic topic,
                          String notes, LocalDate solvedOn, int repetitions, int intervalDays, double ease,
                          LocalDate nextReviewOn, Integer lastQuality, long dueInDays) {

    public static ProblemView of(Problem p, LocalDate today) {
        return new ProblemView(p.getId(), p.getTitle(), p.getUrl(), p.getPlatform(), p.getDifficulty(), p.getTopic(),
                p.getNotes(), p.getSolvedOn(), p.getRepetitions(), p.getIntervalDays(), p.getEaseX100() / 100.0,
                p.getNextReviewOn(), p.getLastQuality(), ChronoUnit.DAYS.between(today, p.getNextReviewOn()));
    }
}
