package com.placementprep.config;

import com.placementprep.core.Sm2;
import com.placementprep.core.Stage;
import com.placementprep.domain.Application;
import com.placementprep.domain.Difficulty;
import com.placementprep.domain.Platform;
import com.placementprep.domain.Problem;
import com.placementprep.domain.Review;
import com.placementprep.domain.Source;
import com.placementprep.domain.StageChange;
import com.placementprep.domain.Topic;
import com.placementprep.domain.User;
import com.placementprep.repository.ApplicationRepository;
import com.placementprep.repository.ProblemRepository;
import com.placementprep.repository.ReviewRepository;
import com.placementprep.repository.StageChangeRepository;
import com.placementprep.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Seeds a demo account (demo@prep.in / demo1234) with five months of realistic practice history and
 * a placement-season application board, so the dashboard has something to show on first run.
 * Disabled with {@code DEMO_DATA=false}. The data is generated from a fixed seed relative to
 * "today", so it always looks current and is the same on every start.
 */
@Configuration
@ConditionalOnProperty(name = "app.demo-data", havingValue = "true")
public class DemoData {

    public static final String DEMO_EMAIL = "demo@prep.in";
    public static final String DEMO_PASSWORD = "demo1234";

    private static final Logger log = LoggerFactory.getLogger(DemoData.class);

    private record App(String company, String title, String location, Source source, String lpa,
                       Stage reached, boolean rejected, String nextStep, int nextStepInDays) {
    }

    private static final List<App> APPS = List.of(
            new App("Persistent Systems", "Graduate Engineer Trainee", "Pune", Source.CAMPUS, "6.50",
                    Stage.INTERVIEW, false, "Technical round 2", 2),
            new App("Zensar Technologies", "Associate Software Engineer", "Pune", Source.NAUKRI, "4.50",
                    Stage.OA, false, "HackerRank online test", 1),
            new App("Infosys", "Systems Engineer", "Pune", Source.CAMPUS, "3.60",
                    Stage.OFFER, false, null, 0),
            new App("TCS", "Digital Engineer", "Hyderabad", Source.CAMPUS, "7.00",
                    Stage.INTERVIEW, true, null, 0),
            new App("Accenture", "Associate Software Engineer", "Hyderabad", Source.LINKEDIN, "4.50",
                    Stage.APPLIED, false, null, 0),
            new App("Cognizant", "Programmer Analyst Trainee", "Pune", Source.CAMPUS, "4.00",
                    Stage.OA, true, null, 0),
            new App("Bajaj Finserv Health", "Java Developer", "Pune", Source.LINKEDIN, "8.00",
                    Stage.INTERVIEW, false, "HR round", 5),
            new App("Qualys", "Associate Software Engineer", "Pune", Source.REFERRAL, "9.00",
                    Stage.OA, false, "Coding round on CodeSignal", 3),
            new App("Amdocs", "Software Developer", "Pune", Source.COMPANY_SITE, "6.00",
                    Stage.APPLIED, false, null, 0),
            new App("Mphasis", "Software Engineer Trainee", "Pune", Source.NAUKRI, "3.80",
                    Stage.APPLIED, true, null, 0),
            new App("Deloitte USI", "Analyst - Technology", "Hyderabad", Source.LINKEDIN, "7.60",
                    Stage.OA, false, "Aptitude + coding test", 6),
            new App("Wipro", "Project Engineer", "Hyderabad", Source.CAMPUS, "3.50",
                    Stage.APPLIED, false, null, 0),
            new App("Darwinbox", "Software Engineer - Backend", "Hyderabad", Source.LINKEDIN, "12.00",
                    Stage.WISHLIST, false, null, 0),
            new App("Hexaware", "Graduate Engineer Trainee", "Pune", Source.INDEED, "4.00",
                    Stage.WISHLIST, false, null, 0));

    private record P(String title, Platform platform, Difficulty difficulty, Topic topic) {
    }

    private static final List<P> PROBLEMS = List.of(
            new P("Two Sum", Platform.LEETCODE, Difficulty.EASY, Topic.HASHING),
            new P("Valid Anagram", Platform.LEETCODE, Difficulty.EASY, Topic.HASHING),
            new P("Best Time to Buy and Sell Stock", Platform.LEETCODE, Difficulty.EASY, Topic.ARRAYS),
            new P("Kadane's Algorithm", Platform.GFG, Difficulty.MEDIUM, Topic.ARRAYS),
            new P("Valid Parentheses", Platform.LEETCODE, Difficulty.EASY, Topic.STACK_QUEUE),
            new P("Reverse Linked List", Platform.LEETCODE, Difficulty.EASY, Topic.LINKED_LIST),
            new P("Merge Two Sorted Lists", Platform.LEETCODE, Difficulty.EASY, Topic.LINKED_LIST),
            new P("Binary Search", Platform.LEETCODE, Difficulty.EASY, Topic.BINARY_SEARCH),
            new P("Climbing Stairs", Platform.LEETCODE, Difficulty.EASY, Topic.DP),
            new P("Maximum Depth of Binary Tree", Platform.LEETCODE, Difficulty.EASY, Topic.TREES),
            new P("Invert Binary Tree", Platform.LEETCODE, Difficulty.EASY, Topic.TREES),
            new P("Second Highest Salary", Platform.LEETCODE, Difficulty.MEDIUM, Topic.SQL),
            new P("Group Anagrams", Platform.LEETCODE, Difficulty.MEDIUM, Topic.HASHING),
            new P("Top K Frequent Elements", Platform.LEETCODE, Difficulty.MEDIUM, Topic.HEAPS),
            new P("Product of Array Except Self", Platform.LEETCODE, Difficulty.MEDIUM, Topic.ARRAYS),
            new P("Longest Substring Without Repeating Characters", Platform.LEETCODE, Difficulty.MEDIUM,
                    Topic.SLIDING_WINDOW),
            new P("Container With Most Water", Platform.LEETCODE, Difficulty.MEDIUM, Topic.TWO_POINTERS),
            new P("3Sum", Platform.LEETCODE, Difficulty.MEDIUM, Topic.TWO_POINTERS),
            new P("Min Stack", Platform.LEETCODE, Difficulty.MEDIUM, Topic.STACK_QUEUE),
            new P("Daily Temperatures", Platform.LEETCODE, Difficulty.MEDIUM, Topic.STACK_QUEUE),
            new P("Next Greater Element", Platform.GFG, Difficulty.MEDIUM, Topic.STACK_QUEUE),
            new P("Search in Rotated Sorted Array", Platform.LEETCODE, Difficulty.MEDIUM, Topic.BINARY_SEARCH),
            new P("Koko Eating Bananas", Platform.LEETCODE, Difficulty.MEDIUM, Topic.BINARY_SEARCH),
            new P("Linked List Cycle II", Platform.LEETCODE, Difficulty.MEDIUM, Topic.LINKED_LIST),
            new P("LRU Cache", Platform.LEETCODE, Difficulty.MEDIUM, Topic.LINKED_LIST),
            new P("Binary Tree Level Order Traversal", Platform.LEETCODE, Difficulty.MEDIUM, Topic.TREES),
            new P("Validate Binary Search Tree", Platform.LEETCODE, Difficulty.MEDIUM, Topic.TREES),
            new P("Lowest Common Ancestor of a Binary Tree", Platform.LEETCODE, Difficulty.MEDIUM, Topic.TREES),
            new P("Kth Smallest Element in a BST", Platform.LEETCODE, Difficulty.MEDIUM, Topic.TREES),
            new P("Number of Islands", Platform.LEETCODE, Difficulty.MEDIUM, Topic.GRAPHS),
            new P("Rotting Oranges", Platform.LEETCODE, Difficulty.MEDIUM, Topic.GRAPHS),
            new P("Course Schedule", Platform.LEETCODE, Difficulty.MEDIUM, Topic.GRAPHS),
            new P("Detect Cycle in an Undirected Graph", Platform.GFG, Difficulty.MEDIUM, Topic.GRAPHS),
            new P("Dijkstra's Shortest Path", Platform.GFG, Difficulty.MEDIUM, Topic.GRAPHS),
            new P("House Robber", Platform.LEETCODE, Difficulty.MEDIUM, Topic.DP),
            new P("Coin Change", Platform.LEETCODE, Difficulty.MEDIUM, Topic.DP),
            new P("Longest Increasing Subsequence", Platform.LEETCODE, Difficulty.MEDIUM, Topic.DP),
            new P("Longest Common Subsequence", Platform.LEETCODE, Difficulty.MEDIUM, Topic.DP),
            new P("0-1 Knapsack Problem", Platform.GFG, Difficulty.MEDIUM, Topic.DP),
            new P("Partition Equal Subset Sum", Platform.LEETCODE, Difficulty.MEDIUM, Topic.DP),
            new P("Word Break", Platform.LEETCODE, Difficulty.MEDIUM, Topic.DP),
            new P("Jump Game", Platform.LEETCODE, Difficulty.MEDIUM, Topic.GREEDY),
            new P("Merge Intervals", Platform.LEETCODE, Difficulty.MEDIUM, Topic.GREEDY),
            new P("Non-overlapping Intervals", Platform.LEETCODE, Difficulty.MEDIUM, Topic.GREEDY),
            new P("Kth Largest Element in an Array", Platform.LEETCODE, Difficulty.MEDIUM, Topic.HEAPS),
            new P("Subsets", Platform.LEETCODE, Difficulty.MEDIUM, Topic.RECURSION),
            new P("Permutations", Platform.LEETCODE, Difficulty.MEDIUM, Topic.RECURSION),
            new P("Combination Sum", Platform.LEETCODE, Difficulty.MEDIUM, Topic.RECURSION),
            new P("Rotate Image", Platform.LEETCODE, Difficulty.MEDIUM, Topic.ARRAYS),
            new P("Spiral Matrix", Platform.LEETCODE, Difficulty.MEDIUM, Topic.ARRAYS),
            new P("Longest Palindromic Substring", Platform.LEETCODE, Difficulty.MEDIUM, Topic.STRINGS),
            new P("String Compression", Platform.LEETCODE, Difficulty.MEDIUM, Topic.STRINGS),
            new P("Rank Scores", Platform.LEETCODE, Difficulty.MEDIUM, Topic.SQL),
            new P("Department Top Three Salaries", Platform.LEETCODE, Difficulty.HARD, Topic.SQL),
            new P("Trapping Rain Water", Platform.LEETCODE, Difficulty.HARD, Topic.TWO_POINTERS),
            new P("Sliding Window Maximum", Platform.LEETCODE, Difficulty.HARD, Topic.SLIDING_WINDOW),
            new P("Minimum Window Substring", Platform.LEETCODE, Difficulty.HARD, Topic.SLIDING_WINDOW),
            new P("Find Median from Data Stream", Platform.LEETCODE, Difficulty.HARD, Topic.HEAPS),
            new P("Edit Distance", Platform.LEETCODE, Difficulty.MEDIUM, Topic.DP),
            new P("N-Queens", Platform.LEETCODE, Difficulty.HARD, Topic.RECURSION),
            new P("Word Ladder", Platform.LEETCODE, Difficulty.HARD, Topic.GRAPHS),
            new P("Merge k Sorted Lists", Platform.LEETCODE, Difficulty.HARD, Topic.HEAPS));

    @Bean
    public CommandLineRunner seedDemoData(UserRepository users, ApplicationRepository applications,
                                          StageChangeRepository stageChanges, ProblemRepository problems,
                                          ReviewRepository reviews, PasswordEncoder passwordEncoder,
                                          TransactionTemplate tx, Clock clock,
                                          @Value("${app.zone:Asia/Kolkata}") String zone) {
        return args -> tx.executeWithoutResult(status -> {
            if (users.existsByEmailIgnoreCase(DEMO_EMAIL)) {
                return;
            }
            User demo = users.save(new User("Pallavi (demo)", DEMO_EMAIL, passwordEncoder.encode(DEMO_PASSWORD)));
            LocalDate today = LocalDate.now(clock);
            ZoneId zoneId = ZoneId.of(zone);
            Random random = new Random(42);

            seedApplications(demo, applications, stageChanges, today, zoneId, random);
            int[] counts = seedPractice(demo, problems, reviews, today, random);
            log.info("Demo data ready: {} / {} ({} applications, {} problems, {} reviews)",
                    DEMO_EMAIL, DEMO_PASSWORD, APPS.size(), counts[0], counts[1]);
        });
    }

    private static void seedApplications(User demo, ApplicationRepository applications,
                                         StageChangeRepository stageChanges, LocalDate today, ZoneId zone,
                                         Random random) {
        for (App a : APPS) {
            Stage current = a.rejected() ? Stage.REJECTED : a.reached();
            Application app = new Application(demo, a.company(), a.title(), a.source(), current);
            app.setLocation(a.location());
            app.setPackageLpa(new BigDecimal(a.lpa()));
            LocalDate day = today.minusDays(20 + random.nextInt(45));
            if (a.reached().isSubmitted()) {
                app.setAppliedOn(day);
            }
            if (a.nextStep() != null) {
                app.setNextStep(a.nextStep());
                app.setNextStepOn(today.plusDays(a.nextStepInDays()));
            }
            app = applications.save(app);

            List<Stage> path = new ArrayList<>();
            for (Stage s : Stage.values()) {
                if (s.isPipeline() && s.ordinal() <= a.reached().ordinal()
                        && (s != Stage.WISHLIST || a.reached() == Stage.WISHLIST)) {
                    path.add(s);
                }
            }
            if (a.rejected()) {
                path.add(Stage.REJECTED);
            }
            Stage previous = null;
            for (Stage s : path) {
                stageChanges.save(new StageChange(app, previous, s, day.atTime(LocalTime.of(10, 30)).atZone(zone)
                        .toInstant()));
                previous = s;
                day = day.plusDays(3 + random.nextInt(7));
                if (day.isAfter(today)) {
                    day = today;
                }
            }
        }
    }

    /**
     * Simulates ~150 days of practice: problems solved on most days (more often as placement
     * season gets closer) and every revision done on its SM-2 due date, mostly remembered well.
     * Some revisions are left due today so the revision queue is not empty.
     */
    private static int[] seedPractice(User demo, ProblemRepository problems, ReviewRepository reviews,
                                      LocalDate today, Random random) {
        // A new problem on each of the last 12 days (a live streak), none on the 13th day back, and the
        // remaining problems are spread over the earlier months, denser closer to today.
        int days = 150;
        int streakDays = 12;
        List<LocalDate> solveDays = new ArrayList<>();
        List<double[]> earlier = new ArrayList<>();
        for (int back = streakDays + 2; back <= days; back++) {
            double weight = 0.25 + (days - back) / (double) days;
            earlier.add(new double[]{back, Math.pow(random.nextDouble(), 1.0 / weight)});
        }
        earlier.sort((x, y) -> Double.compare(y[1], x[1]));
        for (int k = 0; k < PROBLEMS.size() - streakDays; k++) {
            solveDays.add(today.minusDays((long) earlier.get(k)[0]));
        }
        for (int back = streakDays; back >= 1; back--) {
            solveDays.add(today.minusDays(back));
        }
        solveDays.sort(null);

        int reviewCount = 0;
        int created = 0;
        List<Problem> saved = new ArrayList<>();
        for (int i = 0; i < PROBLEMS.size(); i++) {
            LocalDate solvedOn = solveDays.get(i);
            P p = PROBLEMS.get(i);
            Problem problem = new Problem(demo, p.title(), p.platform(), p.difficulty(), p.topic(), solvedOn);
            problem.setUrl(url(p));
            Sm2.State state = new Sm2.State(1, 1, Sm2.INITIAL_EASE);
            LocalDate next = solvedOn.plusDays(1);
            List<Review> history = new ArrayList<>();
            while (next.isBefore(today)) {
                int quality = pickQuality(p.difficulty(), random);
                Sm2.Result r = Sm2.review(state, quality, next);
                history.add(new Review(problem, quality, next, r.state().intervalDays()));
                state = r.state();
                next = r.nextReview();
            }
            Integer last = history.isEmpty() ? null : history.get(history.size() - 1).getQuality();
            problem.applySchedule(state, next, last);
            saved.add(problems.save(problem));
            reviews.saveAll(history);
            reviewCount += history.size();
            created++;
        }
        // Pull the five soonest revisions forward to today so the revision queue has work in it.
        saved.sort(java.util.Comparator.comparing(Problem::getNextReviewOn));
        for (Problem p : saved.subList(0, Math.min(5, saved.size()))) {
            p.applySchedule(p.schedule(), today, p.getLastQuality());
        }
        // One problem solved today so the daily goal ring has started.
        Problem todays = new Problem(demo, "Longest Consecutive Sequence", Platform.LEETCODE, Difficulty.MEDIUM,
                Topic.HASHING, today);
        todays.setUrl("https://leetcode.com/problems/longest-consecutive-sequence/");
        todays.applySchedule(new Sm2.State(1, 1, Sm2.INITIAL_EASE), today.plusDays(1), null);
        problems.save(todays);
        return new int[]{created + 1, reviewCount};
    }

    private static int pickQuality(Difficulty difficulty, Random random) {
        int roll = random.nextInt(100);
        int forgetChance = switch (difficulty) {
            case EASY -> 4;
            case MEDIUM -> 10;
            case HARD -> 20;
        };
        if (roll < forgetChance) {
            return 2;
        }
        if (roll < forgetChance + 25) {
            return 3;
        }
        return roll < 85 ? 4 : 5;
    }

    private static String url(P p) {
        String slug = p.title().toLowerCase().replace("'", "").replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return switch (p.platform()) {
            case LEETCODE -> "https://leetcode.com/problems/" + slug + "/";
            case GFG -> "https://www.geeksforgeeks.org/problems/" + slug + "/1";
            default -> null;
        };
    }
}
