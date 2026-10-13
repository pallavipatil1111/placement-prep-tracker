package com.placementprep.service;

import com.placementprep.core.Stage;
import com.placementprep.domain.Application;
import com.placementprep.domain.User;
import com.placementprep.repository.ApplicationRepository;
import com.placementprep.repository.StageChangeRepository;
import com.placementprep.repository.UserRepository;
import com.placementprep.domain.StageChange;
import com.placementprep.web.dto.ApplicationRequest;
import com.placementprep.web.dto.ApplicationView;
import com.placementprep.web.dto.StageChangeView;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The application board: create, edit, move between stages and keep the history of every move. */
@Service
public class ApplicationService {

    private final ApplicationRepository applications;
    private final StageChangeRepository stageChanges;
    private final UserRepository users;
    private final Clock clock;

    public ApplicationService(ApplicationRepository applications, StageChangeRepository stageChanges,
                              UserRepository users, Clock clock) {
        this.applications = applications;
        this.stageChanges = stageChanges;
        this.users = users;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<ApplicationView> list(Long userId, Stage stage) {
        List<Application> found = stage == null
                ? applications.findByUserIdOrderByUpdatedAtDesc(userId)
                : applications.findByUserIdAndStageOrderByUpdatedAtDesc(userId, stage);
        return found.stream().map(ApplicationView::of).toList();
    }

    @Transactional(readOnly = true)
    public ApplicationView get(Long userId, Long id) {
        return ApplicationView.of(owned(userId, id));
    }

    @Transactional
    public ApplicationView create(Long userId, ApplicationRequest request) {
        User user = users.findById(userId).orElseThrow(() -> ApiException.unauthorized("Account no longer exists"));
        Stage stage = request.stage() == null ? Stage.APPLIED : request.stage();
        Application app = new Application(user, request.company().trim(), request.jobTitle().trim(),
                request.source(), stage);
        copyDetails(request, app);
        if (stage.isSubmitted() && app.getAppliedOn() == null) {
            app.setAppliedOn(today());
        }
        Application saved = applications.save(app);
        stageChanges.save(new StageChange(saved, null, stage, clock.instant()));
        return ApplicationView.of(saved);
    }

    @Transactional
    public ApplicationView update(Long userId, Long id, ApplicationRequest request) {
        Application app = owned(userId, id);
        app.setCompany(request.company().trim());
        app.setJobTitle(request.jobTitle().trim());
        app.setSource(request.source());
        copyDetails(request, app);
        if (request.stage() != null && request.stage() != app.getStage()) {
            changeStage(app, request.stage());
        }
        return ApplicationView.of(applications.save(app));
    }

    /** Moving to the stage it is already in is a no-op, so retried drag-and-drops are harmless. */
    @Transactional
    public ApplicationView move(Long userId, Long id, Stage stage) {
        if (stage == null) {
            throw ApiException.badRequest("Stage is required");
        }
        Application app = owned(userId, id);
        if (app.getStage() == stage) {
            return ApplicationView.of(app);
        }
        changeStage(app, stage);
        return ApplicationView.of(applications.save(app));
    }

    @Transactional(readOnly = true)
    public List<StageChangeView> history(Long userId, Long id) {
        owned(userId, id);
        return stageChanges.findByApplicationIdOrderByChangedAtAscIdAsc(id).stream().map(StageChangeView::of).toList();
    }

    @Transactional
    public void delete(Long userId, Long id) {
        Application app = owned(userId, id);
        stageChanges.deleteByApplicationId(app.getId());
        applications.delete(app);
    }

    private void changeStage(Application app, Stage to) {
        Stage from = app.getStage();
        app.setStage(to);
        if (to.isSubmitted() && to != Stage.REJECTED && app.getAppliedOn() == null) {
            app.setAppliedOn(today());
        }
        stageChanges.save(new StageChange(app, from, to, clock.instant()));
    }

    private static void copyDetails(ApplicationRequest request, Application app) {
        app.setLocation(blankToNull(request.location()));
        app.setPackageLpa(request.packageLpa());
        app.setJobUrl(blankToNull(request.jobUrl()));
        app.setAppliedOn(request.appliedOn() != null ? request.appliedOn() : app.getAppliedOn());
        app.setNextStep(blankToNull(request.nextStep()));
        app.setNextStepOn(request.nextStepOn());
        app.setNotes(blankToNull(request.notes()));
    }

    private Application owned(Long userId, Long id) {
        return applications.findByIdAndUserId(id, userId).orElseThrow(() -> ApiException.notFound("Application"));
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
