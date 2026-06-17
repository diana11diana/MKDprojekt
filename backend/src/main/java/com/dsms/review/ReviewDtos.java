package com.dsms.review;

import com.dsms.user.UserRole;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class ReviewDtos {

    private ReviewDtos() {
    }

    public record CreateReviewRequest(
            @NotNull Long classId,
            @Min(1) @Max(5) int rating,
            @Size(max = 2000) String comment
    ) {
    }

    public record CreateReplyRequest(
            @NotBlank @Size(max = 2000) String body
    ) {
    }

    public record ReviewReplyResponse(
            Long id,
            Long authorUserId,
            String authorName,
            UserRole authorRole,
            String body,
            Instant createdAt
    ) {
        public static ReviewReplyResponse from(ReviewReply reply) {
            return new ReviewReplyResponse(
                    reply.getId(),
                    reply.getAuthor().getId(),
                    reply.getAuthor().getFirstName() + " " + reply.getAuthor().getLastName(),
                    reply.getAuthorRole(),
                    reply.getBody(),
                    reply.getCreatedAt()
            );
        }
    }

    public record ReviewResponse(
            Long id,
            Long classId,
            String classTitle,
            Long instructorId,
            String instructorName,
            Long clientId,
            String clientName,
            int rating,
            String comment,
            Instant createdAt,
            List<ReviewReplyResponse> replies
    ) {
        public static ReviewResponse from(Review review) {
            return new ReviewResponse(
                    review.getId(),
                    review.getClassSession().getId(),
                    review.getClassSession().getTitle(),
                    review.getClassSession().getInstructorId(),
                    review.getClassSession().getInstructorName(),
                    review.getUser().getId(),
                    review.getUser().getFirstName() + " " + review.getUser().getLastName(),
                    review.getRating(),
                    review.getComment(),
                    review.getCreatedAt(),
                    review.getReplies().stream().map(ReviewReplyResponse::from).toList()
            );
        }
    }
}
