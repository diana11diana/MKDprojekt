package com.dsms.review;

import com.dsms.auth.AuthException;
import com.dsms.booking.Reservation;
import com.dsms.booking.ReservationRepository;
import com.dsms.instructor.InstructorProfile;
import com.dsms.instructor.InstructorProfileRepository;
import com.dsms.notification.NotificationService;
import com.dsms.review.ReviewDtos.CreateReplyRequest;
import com.dsms.review.ReviewDtos.CreateReviewRequest;
import com.dsms.schedule.ClassLevel;
import com.dsms.schedule.ClassSession;
import com.dsms.schedule.ClassSessionRepository;
import com.dsms.user.User;
import com.dsms.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ReviewReplyRepository reviewReplyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ClassSessionRepository classSessionRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private InstructorProfileRepository instructorProfileRepository;

    @Mock
    private NotificationService notificationService;

    @Test
    void clientCanCreateReviewForPastBookedClass() {
        User client = client();
        ClassSession classSession = classSession();
        Reservation reservation = new Reservation(client, classSession, null);
        Review saved = new Review(client, classSession, 5, "Super");
        ReflectionTestUtils.setField(saved, "id", 44L);

        when(userRepository.findByEmail(client.getEmail())).thenReturn(Optional.of(client));
        when(classSessionRepository.findById(9L)).thenReturn(Optional.of(classSession));
        when(reservationRepository.findByUserIdAndClassSessionId(7L, 9L)).thenReturn(Optional.of(reservation));
        when(reviewRepository.findByUserIdAndClassSessionId(7L, 9L)).thenReturn(Optional.empty());
        when(reviewRepository.save(any(Review.class))).thenReturn(saved);

        ReviewService service = service();

        var response = service.createMine(client.getEmail(), new CreateReviewRequest(9L, 5, "Super"));

        assertThat(response.id()).isEqualTo(44L);
        assertThat(response.rating()).isEqualTo(5);
        assertThat(response.classId()).isEqualTo(9L);
    }

    @Test
    void clientCannotReviewSameClassTwice() {
        User client = client();
        ClassSession classSession = classSession();
        Reservation reservation = new Reservation(client, classSession, null);
        Review existing = new Review(client, classSession, 4, "Było dobrze");

        when(userRepository.findByEmail(client.getEmail())).thenReturn(Optional.of(client));
        when(classSessionRepository.findById(9L)).thenReturn(Optional.of(classSession));
        when(reservationRepository.findByUserIdAndClassSessionId(7L, 9L)).thenReturn(Optional.of(reservation));
        when(reviewRepository.findByUserIdAndClassSessionId(7L, 9L)).thenReturn(Optional.of(existing));

        ReviewService service = service();

        assertThatThrownBy(() -> service.createMine(client.getEmail(), new CreateReviewRequest(9L, 5, "Jeszcze raz")))
                .isInstanceOf(AuthException.class);

        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void instructorCanReplyToOwnReview() {
        User client = client();
        User instructorUser = instructorUser();
        ClassSession classSession = classSession();
        Review review = new Review(client, classSession, 5, "Super atmosfera");
        ReflectionTestUtils.setField(review, "id", 100L);
        InstructorProfile profile = new InstructorProfile(instructorUser, "Salsa", null);
        ReflectionTestUtils.setField(profile, "id", 5L);

        when(userRepository.findByEmail(instructorUser.getEmail())).thenReturn(Optional.of(instructorUser));
        when(instructorProfileRepository.findByUserId(12L)).thenReturn(Optional.of(profile));
        when(reviewRepository.findById(100L)).thenReturn(Optional.of(review));

        ReviewService service = service();

        var response = service.replyAsInstructor(100L, instructorUser.getEmail(), new CreateReplyRequest("Dziękuję!"));

        assertThat(response.replies()).hasSize(1);
        assertThat(response.replies().get(0).body()).isEqualTo("Dziękuję!");
    }

    private ReviewService service() {
        return new ReviewService(
                reviewRepository,
                reviewReplyRepository,
                userRepository,
                classSessionRepository,
                reservationRepository,
                instructorProfileRepository,
                notificationService
        );
    }

    private static User client() {
        User user = new User("Marta", "Nowak", "marta@example.com", "123456789", "hash");
        user.verifyEmail();
        ReflectionTestUtils.setField(user, "id", 7L);
        return user;
    }

    private static User instructorUser() {
        User user = new User("Anna", "Kowalska", "anna@example.com", "999999999", "hash");
        user.verifyEmail();
        user.changeRole(com.dsms.user.UserRole.INSTRUCTOR);
        ReflectionTestUtils.setField(user, "id", 12L);
        return user;
    }

    private static ClassSession classSession() {
        ClassSession session = new ClassSession(
                "Salsa Start",
                "Podstawy",
                "Salsa",
                ClassLevel.BEGINNER,
                5L,
                "Anna Kowalska",
                16,
                Instant.now().minusSeconds(7200),
                60
        );
        session.publish();
        ReflectionTestUtils.setField(session, "id", 9L);
        return session;
    }
}
