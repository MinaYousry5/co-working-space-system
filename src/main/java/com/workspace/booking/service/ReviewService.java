package com.workspace.booking.service;

import com.workspace.booking.dto.review.ReviewCreateRequest;
import com.workspace.booking.dto.review.ReviewRatingCountResponse;
import com.workspace.booking.dto.review.ReviewReplyRequest;
import com.workspace.booking.dto.review.ReviewResponse;
import com.workspace.booking.dto.review.ReviewSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ReviewService {
    ReviewResponse create(ReviewCreateRequest request);

    Page<ReviewResponse> getAll(Pageable pageable);

    Page<ReviewResponse> getByRating(Integer rating, Pageable pageable);

    ReviewSummaryResponse getSummary();

    List<ReviewRatingCountResponse> getRatingCounts();

    ReviewResponse respond(Long reviewId, ReviewReplyRequest request);
}
