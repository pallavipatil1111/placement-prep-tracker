package com.placementprep.domain;

import com.placementprep.core.Sm2;
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
import java.time.LocalDate;

/** A solved DSA problem together with its spaced-repetition schedule. */
@Entity
@Table(name = "problems")
public class Problem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 500)
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Platform platform;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Difficulty difficulty;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Topic topic;

    @Column(length = 1000)
    private String notes;

    @Column(name = "solved_on", nullable = false)
    private LocalDate solvedOn;

    @Column(nullable = false)
    private int repetitions;

    @Column(name = "interval_days", nullable = false)
    private int intervalDays;

    @Column(name = "ease_x100", nullable = false)
    private int easeX100 = Sm2.INITIAL_EASE;

    @Column(name = "next_review_on", nullable = false)
    private LocalDate nextReviewOn;

    @Column(name = "last_quality")
    private Integer lastQuality;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Problem() {
    }

    public Problem(User user, String title, Platform platform, Difficulty difficulty, Topic topic, LocalDate solvedOn) {
        this.user = user;
        this.title = title;
        this.platform = platform;
        this.difficulty = difficulty;
        this.topic = topic;
        this.solvedOn = solvedOn;
    }

    public Sm2.State schedule() {
        return new Sm2.State(repetitions, intervalDays, easeX100);
    }

    public void applySchedule(Sm2.State state, LocalDate nextReview, Integer quality) {
        this.repetitions = state.repetitions();
        this.intervalDays = state.intervalDays();
        this.easeX100 = state.easeX100();
        this.nextReviewOn = nextReview;
        this.lastQuality = quality;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public String getTitle() {
        return title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Platform getPlatform() {
        return platform;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public Topic getTopic() {
        return topic;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDate getSolvedOn() {
        return solvedOn;
    }

    public int getRepetitions() {
        return repetitions;
    }

    public int getIntervalDays() {
        return intervalDays;
    }

    public int getEaseX100() {
        return easeX100;
    }

    public LocalDate getNextReviewOn() {
        return nextReviewOn;
    }

    public Integer getLastQuality() {
        return lastQuality;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
