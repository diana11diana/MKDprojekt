package com.dsms.booking;

import com.dsms.auth.AuthException;
import com.dsms.booking.BookingDtos.*;
import com.dsms.notification.NotificationService;
import com.dsms.notification.NotificationType;
import com.dsms.pass.PassService;
import com.dsms.pass.UserPass;
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

@Service
public class BookingService {

    private final ClassSessionRepository classRepository;
    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;
    private final WaitingListRepository waitingListRepository;
    private final PassService passService;
    private final NotificationService notificationService;

    public BookingService(
            ClassSessionRepository classRepository,
            UserRepository userRepository,
            ReservationRepository reservationRepository,
            WaitingListRepository waitingListRepository,
            PassService passService,
            NotificationService notificationService
    ) {
        this.classRepository = classRepository;
        this.userRepository = userRepository;
        this.reservationRepository = reservationRepository;
        this.waitingListRepository = waitingListRepository;
        this.passService = passService;
        this.notificationService = notificationService;
    }

    @Transactional
    public BookingResult book(Long classId, String email) {
        User user = getUser(email);
        ClassSession session = getClassForUpdate(classId);
        validateBookable(session);

        Reservation existing = reservationRepository
                .findByUserIdAndClassSessionId(user.getId(), classId)
                .orElse(null);
        if (existing != null && existing.isActive()) {
            throw new AuthException(HttpStatus.CONFLICT, "You already booked this class");
        }

        WaitingListEntry waiting = waitingListRepository
                .findByUserIdAndClassSessionId(user.getId(), classId)
                .orElse(null);
        if (waiting != null && waiting.getStatus() == WaitingListStatus.WAITING) {
            throw new AuthException(HttpStatus.CONFLICT, "You are already on the waiting list");
        }

        if (!session.hasAvailablePlace()) {
            int position = waitingListRepository
                    .findFirstByClassSessionIdOrderByPositionDesc(classId)
                    .map(entry -> entry.getPosition() + 1)
                    .orElse(1);
            if (waiting == null) {
                waiting = new WaitingListEntry(user, session, position);
            } else {
                waiting.rejoin(position);
            }
            waitingListRepository.save(waiting);
            notificationService.notify(
                    user,
                    NotificationType.WAITLIST_JOINED,
                    "Вы в листе ожидания",
                    "Занятие \"" + session.getTitle() + "\" заполнено. Ваша позиция: " + waiting.getPosition() + "."
            );
            return new BookingResult(
                    BookingResultStatus.WAITLISTED,
                    null,
                    waiting.getId(),
                    waiting.getPosition()
            );
        }

        UserPass pass = passService.reserveUsablePass(user.getId(), session.getStartAt());
        if (existing == null) {
            existing = reservationRepository.save(new Reservation(user, session, pass));
        } else {
            existing.reconfirm(pass);
        }
        passService.reserveVisit(pass, existing);
        session.occupyPlace();
        notificationService.notify(
                user,
                NotificationType.BOOKING_CONFIRMED,
                "Запись подтверждена",
                "Вы записаны на \"" + session.getTitle() + "\"."
        );

        return new BookingResult(BookingResultStatus.CONFIRMED, existing.getId(), null, null);
    }

    @Transactional
    public void cancelReservation(Long classId, String email) {
        User user = getUser(email);
        ClassSession session = getClassForUpdate(classId);
        Reservation reservation = reservationRepository
                .findByUserIdAndClassSessionId(user.getId(), classId)
                .filter(Reservation::isActive)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "Active reservation not found"));

        boolean late = Duration.between(Instant.now(), session.getStartAt()).toHours() < 12;
        reservation.cancel(late);
        session.releasePlace();
        if (!late && reservation.getUserPass() != null) {
            passService.releaseVisit(reservation.getUserPass(), reservation);
        }
        notificationService.notify(
                user,
                NotificationType.RESERVATION_CANCELLED,
                "Запись отменена",
                "Вы отменили запись на \"" + session.getTitle() + "\"."
        );
        promoteFirstEligible(session);
    }

    @Transactional
    public void leaveWaitingList(Long classId, String email) {
        User user = getUser(email);
        WaitingListEntry entry = waitingListRepository
                .findByUserIdAndClassSessionId(user.getId(), classId)
                .filter(item -> item.getStatus() == WaitingListStatus.WAITING)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "Waiting list entry not found"));
        entry.cancel();
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> myReservations(String email) {
        User user = getUser(email);
        return reservationRepository.findByUserIdOrderByClassSessionStartAtDesc(user.getId())
                .stream().map(ReservationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<WaitingListResponse> myWaitingList(String email) {
        User user = getUser(email);
        return waitingListRepository
                .findByUserIdAndStatusOrderByClassSessionStartAtAsc(
                        user.getId(),
                        WaitingListStatus.WAITING
                )
                .stream().map(WaitingListResponse::from).toList();
    }

    private void promoteFirstEligible(ClassSession session) {
        while (session.hasAvailablePlace()) {
            WaitingListEntry next = waitingListRepository
                    .findFirstByClassSessionIdAndStatusOrderByPositionAsc(
                            session.getId(),
                            WaitingListStatus.WAITING
                    )
                    .orElse(null);
            if (next == null) {
                return;
            }

            try {
                UserPass pass = passService.reserveUsablePass(
                        next.getUser().getId(),
                        session.getStartAt()
                );
                Reservation reservation = reservationRepository
                        .findByUserIdAndClassSessionId(
                                next.getUser().getId(),
                                session.getId()
                        )
                        .orElse(null);
                if (reservation == null) {
                    reservation = reservationRepository.save(
                            new Reservation(next.getUser(), session, pass)
                    );
                } else {
                    reservation.reconfirm(pass);
                }
                passService.reserveVisit(pass, reservation);
                session.occupyPlace();
                next.promote();
                notificationService.notify(
                        next.getUser(),
                        NotificationType.WAITLIST_PROMOTED,
                        "Место освободилось",
                        "Вы автоматически записаны на \"" + session.getTitle() + "\" из листа ожидания."
                );
                return;
            } catch (AuthException exception) {
                next.expire();
            }
        }
    }

    private void validateBookable(ClassSession session) {
        if (session.getStatus() != ClassStatus.PUBLISHED) {
            throw new AuthException(HttpStatus.CONFLICT, "Class is not available for booking");
        }
        if (!session.getStartAt().isAfter(Instant.now())) {
            throw new AuthException(HttpStatus.CONFLICT, "Class has already started");
        }
    }

    private ClassSession getClassForUpdate(Long classId) {
        return classRepository.findByIdForUpdate(classId)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "Class not found"));
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "User not found"));
    }
}
