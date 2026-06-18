package com.dsms.instructor;

import com.dsms.booking.Reservation;
import com.dsms.booking.ReservationRepository;
import com.dsms.booking.ReservationStatus;
import com.dsms.booking.WaitingListEntry;
import com.dsms.booking.WaitingListRepository;
import com.dsms.booking.WaitingListStatus;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InstructorServiceTest {

    @Mock
    private InstructorProfileRepository profileRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ClassSessionRepository classSessionRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private WaitingListRepository waitingListRepository;

    @Test
    void dashboardShowsOwnClassesWithParticipantsAndWaitlist() {
        User instructorUser = new User(
                "Anna",
                "Kowalska",
                "anna@example.com",
                "123456789",
                "hash"
        );
        ReflectionTestUtils.setField(instructorUser, "id", 10L);

        InstructorProfile profile = new InstructorProfile(instructorUser, "Salsa", "Grupy i solo");
        ReflectionTestUtils.setField(profile, "id", 5L);

        ClassSession session = new ClassSession(
                "Salsa Start",
                "Podstawy",
                "Salsa",
                ClassLevel.BEGINNER,
                5L,
                "Anna Kowalska",
                12,
                Instant.now().plusSeconds(3600),
                60
        );
        session.publish();
        session.occupyPlace();
        ReflectionTestUtils.setField(session, "id", 33L);

        User client = new User(
                "Marta",
                "Nowak",
                "marta@example.com",
                "987654321",
                "hash"
        );
        ReflectionTestUtils.setField(client, "id", 20L);

        Reservation reservation = new Reservation(client, session, null);
        ReflectionTestUtils.setField(reservation, "id", 70L);

        WaitingListEntry waiting = new WaitingListEntry(client, session, 1);
        ReflectionTestUtils.setField(waiting, "id", 80L);

        when(userRepository.findByEmail("anna@example.com")).thenReturn(Optional.of(instructorUser));
        when(profileRepository.findByUserId(10L)).thenReturn(Optional.of(profile));
        when(classSessionRepository.findByInstructorIdOrderByStartAtAsc(5L)).thenReturn(List.of(session));
        when(reservationRepository.findInstructorParticipants(
                33L,
                java.util.Set.of(
                        ReservationStatus.CONFIRMED,
                        ReservationStatus.ATTENDED,
                        ReservationStatus.NO_SHOW
                )
        ))
                .thenReturn(List.of(reservation));
        when(waitingListRepository.findInstructorEntries(33L, WaitingListStatus.WAITING))
                .thenReturn(List.of(waiting));

        InstructorService service = new InstructorService(
                profileRepository,
                userRepository,
                classSessionRepository,
                reservationRepository,
                waitingListRepository
        );

        InstructorDtos.InstructorDashboardResponse response = service.dashboard("anna@example.com");

        assertThat(response.instructorId()).isEqualTo(5L);
        assertThat(response.classes()).hasSize(1);
        assertThat(response.classes().get(0).bookedPlaces()).isEqualTo(1);
        assertThat(response.classes().get(0).participants()).hasSize(1);
        assertThat(response.classes().get(0).waitingList()).hasSize(1);
        assertThat(response.classes().get(0).participants().get(0).email()).isEqualTo("marta@example.com");
    }

    @Test
    void adminDashboardReturnsSelectedInstructorSchedule() {
        User instructorUser = new User(
                "Jan",
                "Nowicki",
                "jan@example.com",
                "111222333",
                "hash"
        );
        ReflectionTestUtils.setField(instructorUser, "id", 15L);

        InstructorProfile profile = new InstructorProfile(instructorUser, "Bachata", "Wieczorne grupy");
        ReflectionTestUtils.setField(profile, "id", 9L);

        ClassSession session = new ClassSession(
                "Bachata Open",
                "Partnerwork",
                "Bachata",
                ClassLevel.INTERMEDIATE,
                9L,
                "Jan Nowicki",
                16,
                Instant.now().plusSeconds(7200),
                75
        );
        session.publish();
        session.occupyPlace();
        session.occupyPlace();
        ReflectionTestUtils.setField(session, "id", 44L);

        when(profileRepository.findById(9L)).thenReturn(Optional.of(profile));
        when(classSessionRepository.findByInstructorIdOrderByStartAtAsc(9L)).thenReturn(List.of(session));
        when(reservationRepository.findInstructorParticipants(
                44L,
                java.util.Set.of(
                        ReservationStatus.CONFIRMED,
                        ReservationStatus.ATTENDED,
                        ReservationStatus.NO_SHOW
                )
        ))
                .thenReturn(List.of());
        when(waitingListRepository.findInstructorEntries(44L, WaitingListStatus.WAITING))
                .thenReturn(List.of());

        InstructorService service = new InstructorService(
                profileRepository,
                userRepository,
                classSessionRepository,
                reservationRepository,
                waitingListRepository
        );

        InstructorDtos.InstructorDashboardResponse response = service.dashboardForAdmin(9L);

        assertThat(response.instructorId()).isEqualTo(9L);
        assertThat(response.firstName()).isEqualTo("Jan");
        assertThat(response.classes()).hasSize(1);
        assertThat(response.classes().get(0).bookedPlaces()).isEqualTo(2);
        assertThat(response.classes().get(0).availablePlaces()).isEqualTo(14);
    }
}
