package com.db440;

import java.time.LocalDate;

/** One row of the review table. Reviews are read-only once submitted. */
public final class Review {

    /** Allowed ratings, best to worst. Must match chk_review_rating in schema.sql. */
    public static final String[] RATINGS = {"Excellent", "Good", "Fair", "Poor"};

    private final int reviewId;
    private final String reviewer;
    private final int itemId;
    private final String rating;
    private final String comment;
    private final LocalDate reviewDate;

    public Review(int reviewId, String reviewer, int itemId, String rating, String comment, LocalDate reviewDate) {
        this.reviewId = reviewId;
        this.reviewer = reviewer;
        this.itemId = itemId;
        this.rating = rating;
        this.comment = comment;
        this.reviewDate = reviewDate;
    }

    public int getReviewId() {
        return reviewId;
    }

    public String getReviewer() {
        return reviewer;
    }

    public int getItemId() {
        return itemId;
    }

    public String getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }

    public LocalDate getReviewDate() {
        return reviewDate;
    }

    /** One line for the review list: [Good] bob, 2026-10-24: Great laptop. */
    public String listingText() {
        return "[" + rating + "] " + reviewer + ", " + reviewDate + ": " + comment;
    }

}