package com.dsms.instructor;

import com.dsms.booking.Reservation;
import com.dsms.booking.ReservationRepository;
import com.dsms.booking.ReservationStatus;
import com.dsms.booking.WaitingListEntry;
import com.dsms.booking.WaitingListRepository;
import com.dsms.booking.WaitingListStatus;
import com.dsms.instructor.InstructorDtos.UpdateAttendanceRequest;
import com.dsms.notification.NotificationService;
import com.dsms.pass.PassService;
import com.dsms.pass.UserPass;
import com.dsms.schedule.ClassLevel;
import com.dsms.schedule.ClassSession;
import com.dsms.schedule.ClassSessionRepository;
import com.dsms.schedule.ClassStatus;
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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InstructorClassServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private InstructorProfileRepository instructorProfileRepository;

    @Mock
    private ClassSessionRepository classSessionRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private WaitingListRepository waitingListRepository;

    @Mock
    private PassService passService;

    @Mock
    private NotificationService notificationService;

    @Test
    void confirmStartChangesClassToInProgress() {
        User instructorUser = new User(
                "Ola",
                "Lis",
                "ola@example.com",
                "123123123",
                "hash"
        );
        ReflectionTestUtils.setField(instructorUser, "id", 1L);

        InstructorProfile profile = new InstructorProfile(instructorUser, "Salsa", "Opis");
        ReflectionTestUtils.setField(profile, "id", 3L);

        ClassSession session = new ClassSession(
                "Salsa Intro",
                "Podstawy",
                "Salsa",
                ClassLevel.BEGINNER,
                3L,
                "Ola Lis",
                12,
                Instant.now().plusSeconds(120),
                60
        );
        session.publish();
        ReflectionTestUtils.setField(session, "id", 10L);

        when(userRepository.findByEmail("ola@example.com")).thenReturn(Optional.of(instructorUser));
        when(instructorProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));
        when(classSessionRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(session));
        when(reservationRepository.findInstructorParticipants(
                10L,
                Set.of(
                        ReservationStatus.CONFIRMED,
                        ReservationStatus.ATTENDED,
                        ReservationStatus.NO_SHOW
                )
        )).thenReturn(List.of());
        when(waitingListRepository.findInstructorEntries(10L, WaitingListStatus.WAITING)).thenReturn(List.of());

        InstructorClassService service = new InstructorClassService(
                userRepository,
                instructorProfileRepository,
                classSessionRepository,
                reservationRepository,
                waitingListRepository,
                passService,
                notificationService
        );

        var response = service.confirmStart(10L, "ola@example.com");

        assertThat(session.getStatus()).isEqualTo(ClassStatus.IN_PROGRESS);
        assertThat(session.getStartedAt()).isNotNull();
        assertThat(response.status()).isEqualTo(ClassStatus.IN_PROGRESS);
    }

    @Test
    void updateAttendanceMarksReservationAsAttended() {
        User instructorUser = new User(
                "Ola",
                "Lis",
                "ola@example.com",
                "123123123",
                "hash"
        );
        ReflectionTestUtils.setField(instructorUser, "id", 1L);

        InstructorProfile profile = new InstructorProfile(instructorUser, "Salsa", "Opis");
        ReflectionTestUtils.setField(profile, "id", 3L);

        ClassSession session = new ClassSession(
                "Salsa Intro",
                "Podstawy",
                "Salsa",
                ClassLevel.BEGINNER,
                3L,
                "Ola Lis",
                12,
                Instant.now().minusSeconds(60),
                60
        );
        session.publish();
        session.start(Instant.now().minusSeconds(30));
        ReflectionTestUtils.setField(session, "id", 10L);

        User client = new User(
                "Marta",
                "Nowak",
                "marta@example.com",
                "999888777",
                "hash"
        );
        ReflectionTestUtils.setField(client, "id", 5L);

        Reservation reservation = new Reservation(client, session, null);
        ReflectionTestUtils.setField(reservation, "id", 20L);

        when(userRepository.findByEmail("ola@example.com")).thenReturn(Optional.of(instructorUser));
        when(instructorProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));
        when(classSessionRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(session));
        when(reservationRepository.findById(20L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.findInstructorParticipants(
                10L,
                Set.of(
                        ReservationStatus.CONFIRMED,
                        ReservationStatus.ATTENDED,
                        ReservationStatus.NO_SHOW
                )
        )).thenReturn(List.of(reservation));
        when(waitingListRepository.findInstructorEntries(10L, WaitingListStatus.WAITING)).thenReturn(List.of());

        InstructorClassService service = new InstructorClassService(
                userRepository,
                instructorProfileRepository,
                classSessionRepository,
                reservationRepository,
                waitingListRepository,
                passService,
                notificationService
        );

        var response = service.updateAttendance(
                10L,
                20L,
                "ola@example.com",
                new UpdateAttendanceRequest(ReservationStatus.ATTENDED)
        );

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.ATTENDED);
        assertThat(response.participants()).hasSize(1);
        assertThat(response.participants().get(0).status()).isEqualTo(ReservationStatus.ATTENDED);
    }

    @Test
    void autoCancelUnconfirmedClassesCancelsReservationsAndWaitlist() {
        ClassSession session = new ClassSession(
                "Salsa Intro",
                "Podstawy",
                "Salsa",
                ClassLevel.BEGINNER,
                3L,
                "Ola Lis",
                12,
                Instant.now().minusSeconds(600),
                60
        );
        session.publish();
        session.occupyPlace();
        ReflectionTestUtils.setField(session, "id", 10L);

        User bookedClient = new User(
                "Marta",
                "Nowak",
                "marta@example.com",
                "999888777",
                "hash"
        );
        ReflectionTestUtils.setField(bookedClient, "id", 5L);
        UserPass pass = org.mockito.Mockito.mock(UserPass.class);
        Reservation reservation = new Reservation(bookedClient, session, pass);
        ReflectionTestUtils.setField(reservation, "id", 20L);

        User waitingClient = new User(
                "Kasia",
                "Lis",
                "kasia@example.com",
                "111222333",
                "hash"
        );
        ReflectionTestUtils.setField(waitingClient, "id", 6L);
        WaitingListEntry waitingEntry = new WaitingListEntry(waitingClient, session, 1);
        ReflectionTestUtils.setField(waitingEntry, "id", 30L);

        when(classSessionRepository.findIdsByStatusAndStartAtAtMost(any(), any())).thenReturn(List.of(10L));
        when(classSessionRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(session));
        when(reservationRepository.findInstructorParticipants(10L, Set.of(ReservationStatus.CONFIRMED)))
                .thenReturn(List.of(reservation));
        when(waitingListRepository.findInstructorEntries(10L, WaitingListStatus.WAITING))
                .thenReturn(List.of(waitingEntry));

        InstructorClassService service = new InstructorClassService(
                userRepository,
                instructorProfileRepository,
                classSessionRepository,
                reservationRepository,
                waitingListRepository,
                passService,
                notificationService
        );

        service.autoCancelUnconfirmedClasses();

        assertThat(session.getStatus()).isEqualTo(ClassStatus.CANCELLED);
        assertThat(session.getBookedPlaces()).isZero();
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(waitingEntry.getStatus()).isEqualTo(WaitingListStatus.CANCELLED);
        verify(passService).releaseVisit(pass, reservation);
        verify(notificationService, times(2)).notify(any(), any(), any(), any());
        verify(classSessionRepository, never()).findByIdForUpdate(999L);
    }
}
