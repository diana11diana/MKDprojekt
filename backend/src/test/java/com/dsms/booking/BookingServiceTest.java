package com.dsms.booking;

import com.dsms.pass.PassService;
import com.dsms.notification.NotificationService;
import com.dsms.pass.PassType;
import com.dsms.pass.PassTypeKind;
import com.dsms.pass.UserPass;
import com.dsms.schedule.ClassLevel;
import com.dsms.schedule.ClassSession;
import com.dsms.schedule.ClassSessionRepository;
import com.dsms.user.User;
import com.dsms.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static com.dsms.booking.BookingDtos.BookingResultStatus.CONFIRMED;
import static com.dsms.booking.BookingDtos.BookingResultStatus.WAITLISTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private ClassSessionRepository classRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private WaitingListRepository waitingListRepository;

    @Mock
    private PassService passService;

    @Mock
    private NotificationService notificationService;

    @Test
    void bookConfirmsReservationWhenPlaceAndPassAreAvailable() {
        User user = user(42L);
        ClassSession session = session(7L, 2);
        UserPass pass = pass(user);
        when(userRepository.findByEmail("client@example.com")).thenReturn(Optional.of(user));
        when(classRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(session));
        when(reservationRepository.findByUserIdAndClassSessionId(42L, 7L))
                .thenReturn(Optional.empty());
        when(waitingListRepository.findByUserIdAndClassSessionId(42L, 7L))
                .thenReturn(Optional.empty());
        when(passService.reserveUsablePass(42L, session.getStartAt())).thenReturn(pass);
        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> {
                    Reservation reservation = invocation.getArgument(0);
                    ReflectionTestUtils.setField(reservation, "id", 55L);
                    return reservation;
                });
        BookingService service = service();

        BookingDtos.BookingResult result = service.book(7L, "client@example.com");

        assertThat(result.result()).isEqualTo(CONFIRMED);
        assertThat(result.reservationId()).isEqualTo(55L);
        assertThat(session.getBookedPlaces()).isEqualTo(1);
        verify(passService).reserveVisit(any(UserPass.class), any(Reservation.class));
    }

    @Test
    void bookAddsClientToWaitingListWhenClassIsFull() {
        User user = user(42L);
        ClassSession session = session(7L, 1);
        session.occupyPlace();
        when(userRepository.findByEmail("client@example.com")).thenReturn(Optional.of(user));
        when(classRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(session));
        when(reservationRepository.findByUserIdAndClassSessionId(42L, 7L))
                .thenReturn(Optional.empty());
        when(waitingListRepository.findByUserIdAndClassSessionId(42L, 7L))
                .thenReturn(Optional.empty());
        when(waitingListRepository.findFirstByClassSessionIdOrderByPositionDesc(7L))
                .thenReturn(Optional.empty());
        when(waitingListRepository.save(any(WaitingListEntry.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        BookingService service = service();

        BookingDtos.BookingResult result = service.book(7L, "client@example.com");

        assertThat(result.result()).isEqualTo(WAITLISTED);
        assertThat(result.queuePosition()).isEqualTo(1);
        verify(passService, never()).reserveUsablePass(any(), any());
        ArgumentCaptor<WaitingListEntry> captor = ArgumentCaptor.forClass(WaitingListEntry.class);
        verify(waitingListRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(WaitingListStatus.WAITING);
    }

    private BookingService service() {
        return new BookingService(
                classRepository,
                userRepository,
                reservationRepository,
                waitingListRepository,
                passService,
                notificationService
        );
    }

    private static User user(Long id) {
        User user = new User("Client", "One", "client@example.com", null, "hash");
        user.verifyEmail();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private static ClassSession session(Long id, int capacity) {
        ClassSession session = new ClassSession(
                "Salsa",
                "Basics",
                "Salsa",
                ClassLevel.BEGINNER,
                3L,
                "Anna Coach",
                capacity,
                Instant.now().plusSeconds(86_400),
                60
        );
        session.publish();
        ReflectionTestUtils.setField(session, "id", id);
        return session;
    }

    private static UserPass pass(User user) {
        PassType type = new PassType(
                "4 classes",
                null,
                PassTypeKind.LIMITED,
                4,
                30,
                BigDecimal.valueOf(120)
        );
        ReflectionTestUtils.setField(type, "id", 9L);
        UserPass pass = new UserPass(user, type, Instant.now().minusSeconds(60));
        ReflectionTestUtils.setField(pass, "id", 11L);
        return pass;
    }
}
