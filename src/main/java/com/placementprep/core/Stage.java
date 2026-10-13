package com.placementprep.core;

import java.util.List;

/** Where a job application currently is. The first five form the hiring pipeline, in order. */
public enum Stage {
    WISHLIST("Wishlist"),
    APPLIED("Applied"),
    OA("Online test"),
    INTERVIEW("Interview"),
    OFFER("Offer"),
    REJECTED("Rejected");

    /** Stages shown in the conversion funnel (wishlist entries have not been sent yet). */
    public static final List<Stage> FUNNEL = List.of(APPLIED, OA, INTERVIEW, OFFER);

    private final String label;

    Stage(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** True for stages on the forward path (everything except {@link #REJECTED}). */
    public boolean isPipeline() {
        return this != REJECTED;
    }

    /** True once the application has actually been submitted. */
    public boolean isSubmitted() {
        return this != WISHLIST;
    }

    /** Whether this pipeline stage is at or beyond {@code other}. Rejected is never "beyond" anything. */
    public boolean atLeast(Stage other) {
        return isPipeline() && other.isPipeline() && ordinal() >= other.ordinal();
    }
}
