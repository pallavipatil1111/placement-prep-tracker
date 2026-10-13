package com.placementprep.service;

import com.placementprep.core.Sm2;
import com.placementprep.domain.Difficulty;
import com.placementprep.domain.Problem;
import com.placementprep.domain.Review;
import com.placementprep.domain.Topic;
import com.placementprep.domain.User;
import com.placementprep.repository.ProblemRepository;
import com.placementprep.repository.ReviewRepository;
import com.placementprep.repository.UserRepository;
import com.placementprep.web.dto.ProblemRequest;
import com.placementprep.web.dto.ProblemView;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Solved-problem log plus the SM-2 revision queue. */
@Service
public class ProblemService {

    /** Solving a problem counts as the first repetition: it comes back for revision the next day. */
    static final Sm2.State AFTER_FIRST_SOLVE = new Sm2.State(1, 1, Sm2.INITIAL_EASE);

    private final ProblemRepository problems;
    private final ReviewRepository reviews;
    private final UserRepository users;
    private final Clock clock;

    public ProblemService(ProblemRepository problems, ReviewRepository reviews, UserRepository users, Clock clock) {
        this.problems = problems;
        this.reviews = reviews;
        this.users = users;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<ProblemView> list(Long userId, Topic topic, Difficulty difficulty, String query) {
        String q = query == null || query.isBlank() ? null : query.trim();
        LocalDate today = today();
        return problems.search(userId, topic, difficulty, q).stream().map(p -> ProblemView.of(p, today)).toList();
    }

    @Transactional(readOnly = true)
    public List<ProblemView> due(Long userId) {
        LocalDate today = today();
        return problems.findByUserIdAndNextReviewOnLessThanEqualOrderByNextReviewOnAscIdAsc(userId, today).stream()
                .map(p -> ProblemView.of(p, today)).toList();
    }

    @Transactional
    public ProblemView create(Long userId, ProblemRequest request) {
        User user = users.findById(userId).orElseThrow(() -> ApiException.unauthorized("Account no longer exists"));
        LocalDate today = today();
        LocalDate solvedOn = request.solvedOn() == null ? today : request.solvedOn();
        if (solvedOn.isAfter(today)) {
            throw ApiException.badRequest("Solved date cannot be in the future");
        }
        Problem problem = new Problem(user, request.title().trim(), request.platform(), request.difficulty(),
                request.topic(), solvedOn);
        problem.setUrl(ApplicationService.blankToNull(request.url()));
        problem.setNotes(ApplicationService.blankToNull(request.notes()));
        problem.applySchedule(AFTER_FIRST_SOLVE, solvedOn.plusDays(AFTER_FIRST_SOLVE.intervalDays()), null);
        return ProblemView.of(problems.save(problem), today);
    }

    /** Grades a revision and schedules the next one. */
    @Transactional
    public ProblemView review(Long userId, Long problemId, int quality) {
        if (quality < 0 || quality > 5) {
            throw ApiException.badRequest("Quality must be between 0 and 5");
        }
        Problem problem = owned(userId, problemId);
        LocalDate today = today();
        Sm2.Result result = Sm2.review(problem.schedule(), quality, today);
        problem.applySchedule(result.state(), result.nextReview(), quality);
        reviews.save(new Review(problem, quality, today, result.state().intervalDays()));
        return ProblemView.of(problems.save(problem), today);
    }

    @Transactional
    public void delete(Long userId, Long problemId) {
        Problem problem = owned(userId, problemId);
        reviews.deleteByProblemId(problem.getId());
        problems.delete(problem);
    }

    private Problem owned(Long userId, Long id) {
        return problems.findByIdAndUserId(id, userId).orElseThrow(() -> ApiException.notFound("Problem"));
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }
}
