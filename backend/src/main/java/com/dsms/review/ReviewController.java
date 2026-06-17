package com.dsms.review;

import com.dsms.review.ReviewDtos.CreateReplyRequest;
import com.dsms.review.ReviewDtos.CreateReviewRequest;
import com.dsms.review.ReviewDtos.ReviewResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class ReviewController {

    private final ReviewService service;

    public ReviewController(ReviewService service) {
        this.service = service;
    }

    @GetMapping("/me/reviews")
    public List<ReviewResponse> listMine(Authentication authentication) {
        return service.listMine(authentication.getName());
    }

    @PostMapping("/me/reviews")
    public ReviewResponse createMine(
            @Valid @RequestBody CreateReviewRequest request,
            Authentication authentication
    ) {
        return service.createMine(authentication.getName(), request);
    }

    @GetMapping("/instructor/reviews")
    public List<ReviewResponse> listInstructorReviews(Authentication authentication) {
        return service.listInstructorReviews(authentication.getName());
    }

    @PostMapping("/instructor/reviews/{reviewId}/replies")
    public ReviewResponse replyAsInstructor(
            @PathVariable Long reviewId,
            @Valid @RequestBody CreateReplyRequest request,
            Authentication authentication
    ) {
        return service.replyAsInstructor(reviewId, authentication.getName(), request);
    }

    @GetMapping("/admin/reviews")
    public List<ReviewResponse> listAll() {
        return service.listAll();
    }

    @PostMapping("/admin/reviews/{reviewId}/replies")
    public ReviewResponse replyAsAdmin(
            @PathVariable Long reviewId,
            @Valid @RequestBody CreateReplyRequest request,
            Authentication authentication
    ) {
        return service.replyAsAdmin(reviewId, authentication.getName(), request);
    }
}
