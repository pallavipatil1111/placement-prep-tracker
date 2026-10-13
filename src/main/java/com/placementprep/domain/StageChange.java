package com.placementprep.domain;

import com.placementprep.core.Stage;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/** Audit trail of every move on the board; powers the timeline and the conversion funnel. */
@Entity
@Table(name = "stage_changes")
public class StageChange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_stage", length = 20)
    private Stage fromStage;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_stage", nullable = false, length = 20)
    private Stage toStage;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    protected StageChange() {
    }

    public StageChange(Application application, Stage fromStage, Stage toStage, Instant changedAt) {
        this.application = application;
        this.fromStage = fromStage;
        this.toStage = toStage;
        this.changedAt = changedAt;
    }

    public Long getId() {
        return id;
    }

    public Application getApplication() {
        return application;
    }

    public Stage getFromStage() {
        return fromStage;
    }

    public Stage getToStage() {
        return toStage;
    }

    public Instant getChangedAt() {
        return changedAt;
    }
}
