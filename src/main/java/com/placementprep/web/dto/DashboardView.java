package com.placementprep.web.dto;

import com.placementprep.core.Funnel;
import com.placementprep.core.Heatmap;
import com.placementprep.core.Stage;
import com.placementprep.domain.Difficulty;
import com.placementprep.repository.TopicCount;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record DashboardView(
        LocalDate today,
        int dailyGoal,
        int todayCount,
        int currentStreak,
        int longestStreak,
        long totalSolved,
        long dueCount,
        Map<Difficulty, Long> byDifficulty,
        List<TopicCount> byTopic,
        long totalApplications,
        long activeApplications,
        long offers,
        Map<Stage, Long> byStage,
        List<Funnel.Step> funnel,
        List<ApplicationView> upcoming,
        Heatmap.Grid heatmap) {
}
