package com.placementprep.service;

import static com.placementprep.service.TestFixtures.CLOCK;
import static com.placementprep.service.TestFixtures.TODAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.placementprep.core.Stage;
import com.placementprep.domain.Application;
import com.placementprep.domain.Source;
import com.placementprep.domain.StageChange;
import com.placementprep.domain.User;
import com.placementprep.repository.ApplicationRepository;
import com.placementprep.repository.StageChangeRepository;
import com.placementprep.repository.UserRepository;
import com.placementprep.web.dto.ApplicationRequest;
import com.placementprep.web.dto.ApplicationView;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;

class ApplicationServiceTest {

    private ApplicationRepository applications;
    private StageChangeRepository stageChanges;
    private UserRepository users;
    private ApplicationService service;
    private final User user = TestFixtures.user(1L);

    @BeforeEach
    void setUp() {
        applications = mock(ApplicationRepository.class);
        stageChanges = mock(StageChangeRepository.class);
        users = mock(UserRepository.class);
        service = new ApplicationService(applications, stageChanges, users, CLOCK);
        when(users.findById(1L)).thenReturn(Optional.of(user));
        when(applications.save(any())).thenAnswer(inv -> {
            Application a = inv.getArgument(0);
            if (a.getId() == null) {
                a.setId(10L);
            }
            return a;
        });
    }

    private static ApplicationRequest request(Stage stage) {
        return new ApplicationRequest("  Persistent Systems ", "Graduate Engineer Trainee", "Pune", Source.CAMPUS,
                stage, new BigDecimal("6.50"), "https://careers.example.com/123", null, " ", null, "");
    }

    private Application existing(Stage stage) {
        Application a = new Application(user, "Zensar", "ASE", Source.NAUKRI, stage);
        a.setId(5L);
        when(applications.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(a));
        return a;
    }

    @Test
    void createDefaultsToAppliedTodayAndRecordsTheFirstStage() {
        ApplicationView view = service.create(1L, request(null));

        assertThat(view.id()).isEqualTo(10L);
        assertThat(view.company()).isEqualTo("Persistent Systems");
        assertThat(view.stage()).isEqualTo(Stage.APPLIED);
        assertThat(view.appliedOn()).isEqualTo(TODAY);
        assertThat(view.nextStep()).isNull();
        assertThat(view.notes()).isNull();

        ArgumentCaptor<StageChange> change = ArgumentCaptor.forClass(StageChange.class);
        verify(stageChanges).save(change.capture());
        assertThat(change.getValue().getFromStage()).isNull();
        assertThat(change.getValue().getToStage()).isEqualTo(Stage.APPLIED);
        assertThat(change.getValue().getChangedAt()).isEqualTo(CLOCK.instant());
    }

    @Test
    void wishlistEntriesHaveNoApplyDate() {
        ApplicationView view = service.create(1L, request(Stage.WISHLIST));
        assertThat(view.stage()).isEqualTo(Stage.WISHLIST);
        assertThat(view.appliedOn()).isNull();
    }

    @Test
    void movingRecordsTheTransitionAndStampsTheApplyDate() {
        existing(Stage.WISHLIST);

        ApplicationView view = service.move(1L, 5L, Stage.APPLIED);

        assertThat(view.stage()).isEqualTo(Stage.APPLIED);
        assertThat(view.stageLabel()).isEqualTo("Applied");
        assertThat(view.appliedOn()).isEqualTo(TODAY);
        ArgumentCaptor<StageChange> change = ArgumentCaptor.forClass(StageChange.class);
        verify(stageChanges).save(change.capture());
        assertThat(change.getValue().getFromStage()).isEqualTo(Stage.WISHLIST);
        assertThat(change.getValue().getToStage()).isEqualTo(Stage.APPLIED);
    }

    @Test
    void movingToTheSameStageIsANoOp() {
        existing(Stage.OA);

        service.move(1L, 5L, Stage.OA);

        verify(stageChanges, never()).save(any());
        verify(applications, never()).save(any());
    }

    @Test
    void rejectionDoesNotInventAnApplyDate() {
        existing(Stage.WISHLIST);
        assertThat(service.move(1L, 5L, Stage.REJECTED).appliedOn()).isNull();
    }

    @Test
    void updateWithANewStageAlsoRecordsHistory() {
        existing(Stage.APPLIED);
        ApplicationView view = service.update(1L, 5L, request(Stage.INTERVIEW));
        assertThat(view.stage()).isEqualTo(Stage.INTERVIEW);
        assertThat(view.company()).isEqualTo("Persistent Systems");
        assertThat(view.packageLpa()).isEqualByComparingTo(new BigDecimal("6.5"));
        verify(stageChanges).save(any());
    }

    @Test
    void someoneElsesApplicationIsNotFound() {
        when(applications.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.move(1L, 99L, Stage.OFFER))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void deleteRemovesHistoryThenTheApplication() {
        Application app = existing(Stage.OFFER);
        service.delete(1L, 5L);
        verify(stageChanges).deleteByApplicationId(5L);
        verify(applications).delete(app);
    }

    @Test
    void listFiltersByStageWhenGiven() {
        service.list(1L, Stage.OA);
        verify(applications).findByUserIdAndStageOrderByUpdatedAtDesc(1L, Stage.OA);
        service.list(1L, null);
        verify(applications).findByUserIdOrderByUpdatedAtDesc(1L);
    }
}
