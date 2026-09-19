package com.workspace.booking.controller;

import com.workspace.booking.common.ApiResponse;
import com.workspace.booking.dto.review.ReviewCreateRequest;
import com.workspace.booking.dto.review.ReviewRatingCountResponse;
import com.workspace.booking.dto.review.ReviewReplyRequest;
import com.workspace.booking.dto.review.ReviewResponse;
import com.workspace.booking.dto.review.ReviewSummaryResponse;
import com.workspace.booking.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
@Slf4j
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    @Operation(summary = "Create review")
    public ResponseEntity<ApiResponse<ReviewResponse>> create(@Valid @RequestBody ReviewCreateRequest request) {
        log.info("Received create review request userId={}", request.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(reviewService.create(request)));
    }


    @GetMapping
    @Operation(summary = "Get all reviews")
    public ResponseEntity<ApiResponse<Page<ReviewResponse>>> getAll(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(reviewService.getAll(pageable)));
    }

    @GetMapping("/rating/{rating}")
    @Operation(summary = "Get reviews by rating")
    public ResponseEntity<ApiResponse<Page<ReviewResponse>>> getByRating(
            @PathVariable Integer rating,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(reviewService.getByRating(rating, pageable)));
    }

    @GetMapping("/summary")
    @Operation(summary = "Get review average and total count")
    public ResponseEntity<ApiResponse<ReviewSummaryResponse>> getSummary() {
        return ResponseEntity.ok(ApiResponse.success(reviewService.getSummary()));
    }

    @GetMapping("/rating-counts")
    @Operation(summary = "Get total number of reviews for every rating")
    public ResponseEntity<ApiResponse<List<ReviewRatingCountResponse>>> getRatingCounts() {
        return ResponseEntity.ok(ApiResponse.success(reviewService.getRatingCounts()));
    }

    @PostMapping("/{id}/response")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Admin response to review")
    public ResponseEntity<ApiResponse<ReviewResponse>> respond(
            @PathVariable Long id,
            @Valid @RequestBody ReviewReplyRequest request) {
        return ResponseEntity.ok(ApiResponse.success(reviewService.respond(id, request)));
    }
}
