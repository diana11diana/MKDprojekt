package com.dsms.booking;

import java.time.Instant;

public final class BookingDtos {

    private BookingDtos() {
    }

    public enum BookingResultStatus {
        CONFIRMED,
        WAITLISTED
    }

    public record BookingResult(
            BookingResultStatus result,
            Long reservationId,
            Long waitingListId,
            Integer queuePosition
    ) {
    }

    public record ReservationResponse(
            Long id,
            Long classId,
            String classTitle,
            String instructorName,
            Instant startAt,
            ReservationStatus status,
            String passName
    ) {
        public static ReservationResponse from(Reservation reservation) {
            return new ReservationResponse(
                    reservation.getId(),
                    reservation.getClassSession().getId(),
                    reservation.getClassSession().getTitle(),
                    reservation.getClassSession().getInstructorName(),
                    reservation.getClassSession().getStartAt(),
                    reservation.getStatus(),
                    reservation.getUserPass() == null
                            ? null
                            : reservation.getUserPass().getPassType().getName()
            );
        }
    }

    public record WaitingListResponse(
            Long id,
            Long classId,
            String classTitle,
            Instant startAt,
            int position,
            WaitingListStatus status
    ) {
        public static WaitingListResponse from(WaitingListEntry entry) {
            return new WaitingListResponse(
                    entry.getId(),
                    entry.getClassSession().getId(),
                    entry.getClassSession().getTitle(),
                    entry.getClassSession().getStartAt(),
                    entry.getPosition(),
                    entry.getStatus()
            );
        }
    }
}

