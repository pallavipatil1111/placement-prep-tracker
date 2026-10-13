package com.placementprep.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

/** One revision of a problem and how well it was recalled (0-5). */
@Entity
@Table(name = "reviews")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;

    @Column(nullable = false)
    private int quality;

    @Column(name = "reviewed_on", nullable = false)
    private LocalDate reviewedOn;

    @Column(name = "interval_after", nullable = false)
    private int intervalAfter;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Review() {
    }

    public Review(Problem problem, int quality, LocalDate reviewedOn, int intervalAfter) {
        this.problem = problem;
        this.quality = quality;
        this.reviewedOn = reviewedOn;
        this.intervalAfter = intervalAfter;
    }

    public Long getId() {
        return id;
    }

    public Problem getProblem() {
        return problem;
    }

    public int getQuality() {
        return quality;
    }

    public LocalDate getReviewedOn() {
        return reviewedOn;
    }

    public int getIntervalAfter() {
        return intervalAfter;
    }
}
