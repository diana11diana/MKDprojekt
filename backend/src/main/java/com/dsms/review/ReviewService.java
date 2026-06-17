package com.dsms.review;

import com.dsms.auth.AuthException;
import com.dsms.booking.Reservation;
import com.dsms.booking.ReservationRepository;
import com.dsms.booking.ReservationStatus;
import com.dsms.instructor.InstructorProfile;
import com.dsms.instructor.InstructorProfileRepository;
import com.dsms.notification.NotificationService;
import com.dsms.notification.NotificationType;
import com.dsms.review.ReviewDtos.CreateReplyRequest;
import com.dsms.review.ReviewDtos.CreateReviewRequest;
import com.dsms.review.ReviewDtos.ReviewResponse;
import com.dsms.schedule.ClassSession;
import com.dsms.schedule.ClassStatus;
import com.dsms.schedule.ClassSessionRepository;
import com.dsms.user.User;
import com.dsms.user.UserRepository;
import com.dsms.user.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Service
public class ReviewService {

    private static final Set<ReservationStatus> REVIEWABLE_RESERVATION_STATUSES = Set.of(
            ReservationStatus.CONFIRMED,
            ReservationStatus.ATTENDED
    );

    private final ReviewRepository reviewRepository;
    private final ReviewReplyRepository reviewReplyRepository;
    private final UserRepository userRepository;
    private final ClassSessionRepository classSessionRepository;
    private final ReservationRepository reservationRepository;
    private final InstructorProfileRepository instructorProfileRepository;
    private final NotificationService notificationService;

    public ReviewService(
            ReviewRepository reviewRepository,
            ReviewReplyRepository reviewReplyRepository,
            UserRepository userRepository,
            ClassSessionRepository classSessionRepository,
            ReservationRepository reservationRepository,
            InstructorProfileRepository instructorProfileRepository,
            NotificationService notificationService
    ) {
        this.reviewRepository = reviewRepository;
        this.reviewReplyRepository = reviewReplyRepository;
        this.userRepository = userRepository;
        this.classSessionRepository = classSessionRepository;
        this.reservationRepository = reservationRepository;
        this.instructorProfileRepository = instructorProfileRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public ReviewResponse createMine(String email, CreateReviewRequest request) {
        User user = getUser(email);
        ClassSession classSession = classSessionRepository.findById(request.classId())
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "Class not found"));
        validateReviewableClass(classSession);

        Reservation reservation = reservationRepository
                .findByUserIdAndClassSessionId(user.getId(), classSession.getId())
                .orElseThrow(() -> new AuthException(HttpStatus.FORBIDDEN, "You can review only your own classes"));

        if (!REVIEWABLE_RESERVATION_STATUSES.contains(reservation.getStatus())) {
            throw new AuthException(HttpStatus.CONFLICT, "Only completed or attended classes can be reviewed");
        }
        if (reviewRepository.findByUserIdAndClassSessionId(user.getId(), classSession.getId()).isPresent()) {
            throw new AuthException(HttpStatus.CONFLICT, "You already reviewed this class");
        }

        Review review = reviewRepository.save(new Review(
                user,
                classSession,
                request.rating(),
                normalizeOptional(request.comment())
        ));
        return ReviewResponse.from(review);
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> listMine(String email) {
        User user = getUser(email);
        return reviewRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(ReviewResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> listInstructorReviews(String email) {
        User user = getUser(email);
        InstructorProfile profile = instructorProfileRepository.findByUserId(user.getId()).orElse(null);
        if (profile == null) {
            return List.of();
        }
        return reviewRepository.findByClassSessionInstructorIdOrderByCreatedAtDesc(profile.getId())
                .stream()
                .map(ReviewResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> listAll() {
        return reviewRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(ReviewResponse::from)
                .toList();
    }

    @Transactional
    public ReviewResponse replyAsInstructor(Long reviewId, String email, CreateReplyRequest request) {
        User author = getUser(email);
        InstructorProfile profile = instructorProfileRepository.findByUserId(author.getId())
                .orElseThrow(() -> new AuthException(HttpStatus.FORBIDDEN, "Instructor profile not found"));
        Review review = getReview(reviewId);
        if (!profile.getId().equals(review.getClassSession().getInstructorId())) {
            throw new AuthException(HttpStatus.FORBIDDEN, "You can reply only to reviews of your own classes");
        }
        reviewReplyRepository.save(review.addReply(author, request.body().trim()));
        notifyClientAboutReply(review, author, "Instruktor odpowiedział na Twoją opinię.");
        return ReviewResponse.from(review);
    }

    @Transactional
    public ReviewResponse replyAsAdmin(Long reviewId, String email, CreateReplyRequest request) {
        User author = getUser(email);
        if (author.getRole() != UserRole.ADMIN) {
            throw new AuthException(HttpStatus.FORBIDDEN, "Admin access required");
        }
        Review review = getReview(reviewId);
        reviewReplyRepository.save(review.addReply(author, request.body().trim()));
        notifyClientAboutReply(review, author, "Administrator odpowiedział na Twoją opinię.");
        return ReviewResponse.from(review);
    }

    private void validateReviewableClass(ClassSession classSession) {
        if (classSession.getStatus() == ClassStatus.CANCELLED) {
            throw new AuthException(HttpStatus.CONFLICT, "Cancelled classes cannot be reviewed");
        }
        if (!classSession.getStartAt().isBefore(Instant.now())) {
            throw new AuthException(HttpStatus.CONFLICT, "You can review a class only after it starts");
        }
    }

    private void notifyClientAboutReply(Review review, User author, String title) {
        notificationService.notify(
                review.getUser(),
                NotificationType.REVIEW_REPLY_ADDED,
                title,
                author.getFirstName() + " " + author.getLastName()
                        + " odpowiedział(a) na opinię o zajęciach \""
                        + review.getClassSession().getTitle() + "\"."
        );
    }

    private Review getReview(Long reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "Review not found"));
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
