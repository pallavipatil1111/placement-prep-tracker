package com.placementprep.service;

import com.placementprep.core.Funnel;
import com.placementprep.core.Heatmap;
import com.placementprep.core.Stage;
import com.placementprep.core.Streaks;
import com.placementprep.domain.Difficulty;
import com.placementprep.domain.User;
import com.placementprep.repository.ApplicationRepository;
import com.placementprep.repository.DayCount;
import com.placementprep.repository.DifficultyCount;
import com.placementprep.repository.ProblemRepository;
import com.placementprep.repository.ReviewRepository;
import com.placementprep.repository.StageChangeRepository;
import com.placementprep.repository.StageCount;
import com.placementprep.repository.StageVisit;
import com.placementprep.repository.TopicCount;
import com.placementprep.repository.UserRepository;
import com.placementprep.web.dto.ApplicationView;
import com.placementprep.web.dto.DashboardView;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Everything on the dashboard, built from a handful of aggregate queries. */
@Service
public class DashboardService {

    public static final int DEFAULT_WEEKS = 26;

    private final ProblemRepository problems;
    private final ReviewRepository reviews;
    private final ApplicationRepository applications;
    private final StageChangeRepository stageChanges;
    private final UserRepository users;
    private final Clock clock;

    public DashboardService(ProblemRepository problems, ReviewRepository reviews, ApplicationRepository applications,
                            StageChangeRepository stageChanges, UserRepository users, Clock clock) {
        this.problems = problems;
        this.reviews = reviews;
        this.applications = applications;
        this.stageChanges = stageChanges;
        this.users = users;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DashboardView build(Long userId, int weeks) {
        User user = users.findById(userId).orElseThrow(() -> ApiException.unauthorized("Account no longer exists"));
        LocalDate today = LocalDate.now(clock);

        Map<LocalDate, Integer> activity = activityPerDay(userId);
        Streaks.Summary streak = Streaks.of(activity.keySet(), today);
        Heatmap.Grid heatmap = Heatmap.build(activity, today, Math.max(4, Math.min(53, weeks)));

        Map<Difficulty, Long> byDifficulty = new EnumMap<>(Difficulty.class);
        for (Difficulty d : Difficulty.values()) {
            byDifficulty.put(d, 0L);
        }
        for (DifficultyCount c : problems.countByDifficulty(userId)) {
            byDifficulty.put(c.key(), c.count());
        }
        List<TopicCount> byTopic = new ArrayList<>(problems.countByTopic(userId));
        byTopic.sort(Comparator.comparing(TopicCount::count).reversed().thenComparing(c -> c.key().name()));

        Map<Stage, Long> byStage = new LinkedHashMap<>();
        for (Stage s : Stage.values()) {
            byStage.put(s, 0L);
        }
        for (StageCount c : applications.countByStage(userId)) {
            byStage.put(c.key(), c.count());
        }
        long total = byStage.values().stream().mapToLong(Long::longValue).sum();
        long active = byStage.get(Stage.APPLIED) + byStage.get(Stage.OA) + byStage.get(Stage.INTERVIEW);

        Map<Long, List<Stage>> histories = new HashMap<>();
        for (StageVisit v : stageChanges.findVisitsForUser(userId)) {
            histories.computeIfAbsent(v.applicationId(), k -> new ArrayList<>()).add(v.stage());
        }
        List<Funnel.Step> funnel = Funnel.of(histories.values());

        List<ApplicationView> upcoming = applications
                .findTop5ByUserIdAndNextStepOnGreaterThanEqualOrderByNextStepOnAsc(userId, today).stream()
                .filter(a -> a.getStage() != Stage.REJECTED)
                .map(ApplicationView::of).toList();

        return new DashboardView(today, user.getDailyGoal(), activity.getOrDefault(today, 0),
                streak.current(), streak.longest(), problems.countByUserId(userId),
                problems.countByUserIdAndNextReviewOnLessThanEqual(userId, today), byDifficulty, byTopic,
                total, active, byStage.get(Stage.OFFER), byStage, funnel, upcoming, heatmap);
    }

    /** Problems solved plus problems revised, per day. */
    Map<LocalDate, Integer> activityPerDay(Long userId) {
        Map<LocalDate, Integer> activity = new HashMap<>();
        for (DayCount c : problems.solvedPerDay(userId)) {
            activity.merge(c.day(), c.count().intValue(), Integer::sum);
        }
        for (DayCount c : reviews.reviewsPerDay(userId)) {
            activity.merge(c.day(), c.count().intValue(), Integer::sum);
        }
        return activity;
    }
}
