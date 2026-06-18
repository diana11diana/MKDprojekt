package com.dsms.instructor;

import com.dsms.auth.AuthException;
import com.dsms.booking.Reservation;
import com.dsms.booking.ReservationRepository;
import com.dsms.booking.ReservationStatus;
import com.dsms.booking.WaitingListEntry;
import com.dsms.booking.WaitingListRepository;
import com.dsms.booking.WaitingListStatus;
import com.dsms.instructor.InstructorDtos.InstructorClassResponse;
import com.dsms.instructor.InstructorDtos.InstructorParticipantResponse;
import com.dsms.instructor.InstructorDtos.InstructorWaitingListResponse;
import com.dsms.instructor.InstructorDtos.UpdateAttendanceRequest;
import com.dsms.notification.NotificationService;
import com.dsms.notification.NotificationType;
import com.dsms.pass.PassService;
import com.dsms.schedule.ClassSession;
import com.dsms.schedule.ClassSessionRepository;
import com.dsms.schedule.ClassStatus;
import com.dsms.user.User;
import com.dsms.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

@Service
public class InstructorClassService {

    private static final Duration START_CONFIRMATION_WINDOW = Duration.ofMinutes(15);
    private static final Duration AUTO_CANCEL_GRACE_PERIOD = Duration.ofMinutes(5);
    private static final Set<ReservationStatus> VISIBLE_RESERVATION_STATUSES = Set.of(
            ReservationStatus.CONFIRMED,
            ReservationStatus.ATTENDED,
            ReservationStatus.NO_SHOW
    );

    private final UserRepository userRepository;
    private final InstructorProfileRepository instructorProfileRepository;
    private final ClassSessionRepository classSessionRepository;
    private final ReservationRepository reservationRepository;
    private final WaitingListRepository waitingListRepository;
    private final PassService passService;
    private final NotificationService notificationService;

    public InstructorClassService(
            UserRepository userRepository,
            InstructorProfileRepository instructorProfileRepository,
            ClassSessionRepository classSessionRepository,
            ReservationRepository reservationRepository,
            WaitingListRepository waitingListRepository,
            PassService passService,
            NotificationService notificationService
    ) {
        this.userRepository = userRepository;
        this.instructorProfileRepository = instructorProfileRepository;
        this.classSessionRepository = classSessionRepository;
        this.reservationRepository = reservationRepository;
        this.waitingListRepository = waitingListRepository;
        this.passService = passService;
        this.notificationService = notificationService;
    }

    @Transactional
    public InstructorClassResponse confirmStart(Long classId, String email) {
        InstructorProfile profile = getInstructorProfile(email);
        ClassSession session = getOwnedSessionForUpdate(classId, profile.getId());

        if (session.getStatus() != ClassStatus.PUBLISHED) {
            throw new AuthException(HttpStatus.CONFLICT, "Only published classes can be started");
        }

        Instant now = Instant.now();
        Instant earliestStartConfirmation = session.getStartAt().minus(START_CONFIRMATION_WINDOW);
        Instant latestStartConfirmation = session.getStartAt().plus(AUTO_CANCEL_GRACE_PERIOD);
        if (now.isBefore(earliestStartConfirmation)) {
            throw new AuthException(HttpStatus.CONFLICT, "Start can be confirmed only shortly before class");
        }
        if (now.isAfter(latestStartConfirmation)) {
            throw new AuthException(HttpStatus.CONFLICT, "This class was not confirmed in time");
        }

        session.start(now);
        return buildClassResponse(session);
    }

    @Transactional
    public InstructorClassResponse updateAttendance(
            Long classId,
            Long reservationId,
            String email,
            UpdateAttendanceRequest request
    ) {
        InstructorProfile profile = getInstructorProfile(email);
        ClassSession session = getOwnedSessionForUpdate(classId, profile.getId());
        if (session.getStatus() != ClassStatus.IN_PROGRESS) {
            throw new AuthException(HttpStatus.CONFLICT, "Attendance can be updated only after class start");
        }

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "Reservation not found"));
        if (!reservation.getClassSession().getId().equals(session.getId())) {
            throw new AuthException(HttpStatus.NOT_FOUND, "Reservation not found for this class");
        }
        if (!VISIBLE_RESERVATION_STATUSES.contains(reservation.getStatus())) {
            throw new AuthException(HttpStatus.CONFLICT, "This reservation cannot be updated");
        }

        switch (request.status()) {
            case ATTENDED -> reservation.markAttended();
            case NO_SHOW -> reservation.markNoShow();
            case CONFIRMED -> reservation.resetAttendance();
            default -> throw new AuthException(HttpStatus.BAD_REQUEST, "Unsupported attendance status");
        }

        return buildClassResponse(session);
    }

    @Transactional
    public void autoCancelUnconfirmedClasses() {
        Instant cutoff = Instant.now().minus(AUTO_CANCEL_GRACE_PERIOD);
        List<Long> sessionIds = classSessionRepository.findIdsByStatusAndStartAtAtMost(
                ClassStatus.PUBLISHED,
                cutoff
        );

        for (Long sessionId : sessionIds) {
            ClassSession session = classSessionRepository.findByIdForUpdate(sessionId).orElse(null);
            if (session == null
                    || session.getStatus() != ClassStatus.PUBLISHED
                    || session.getStartAt().isAfter(cutoff)) {
                continue;
            }

            autoCancelSession(session);
        }
    }

    private void autoCancelSession(ClassSession session) {
        session.cancel();

        List<Reservation> reservations = reservationRepository.findInstructorParticipants(
                session.getId(),
                Set.of(ReservationStatus.CONFIRMED)
        );
        for (Reservation reservation : reservations) {
            reservation.cancel(false);
            session.releasePlace();
            if (reservation.getUserPass() != null) {
                passService.releaseVisit(reservation.getUserPass(), reservation);
            }
            notificationService.notify(
                    reservation.getUser(),
                    NotificationType.CLASS_CANCELLED,
                    "Zajęcia zostały anulowane",
                    "Zajęcia \"" + session.getTitle()
                            + "\" zostały anulowane, ponieważ instruktor nie potwierdził rozpoczęcia na czas."
            );
        }

        List<WaitingListEntry> waitingEntries = waitingListRepository.findInstructorEntries(
                session.getId(),
                WaitingListStatus.WAITING
        );
        for (WaitingListEntry entry : waitingEntries) {
            entry.cancel();
            notificationService.notify(
                    entry.getUser(),
                    NotificationType.CLASS_CANCELLED,
                    "Zajęcia zostały anulowane",
                    "Zajęcia \"" + session.getTitle()
                            + "\" zostały anulowane, więc lista oczekujących została zamknięta."
            );
        }
    }

    private InstructorClassResponse buildClassResponse(ClassSession session) {
        return InstructorClassResponse.from(
                session,
                reservationRepository.findInstructorParticipants(session.getId(), VISIBLE_RESERVATION_STATUSES)
                        .stream()
                        .map(InstructorParticipantResponse::from)
                        .toList(),
                waitingListRepository.findInstructorEntries(session.getId(), WaitingListStatus.WAITING)
                        .stream()
                        .map(InstructorWaitingListResponse::from)
                        .toList()
        );
    }

    private InstructorProfile getInstructorProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "User not found"));
        return instructorProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new AuthException(HttpStatus.FORBIDDEN, "Instructor profile not found"));
    }

    private ClassSession getOwnedSessionForUpdate(Long classId, Long instructorId) {
        ClassSession session = classSessionRepository.findByIdForUpdate(classId)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "Class not found"));
        if (!instructorId.equals(session.getInstructorId())) {
            throw new AuthException(HttpStatus.FORBIDDEN, "You can manage only your own classes");
        }
        return session;
    }
}
