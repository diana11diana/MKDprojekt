package com.dsms.instructor;

import com.dsms.booking.Reservation;
import com.dsms.booking.ReservationStatus;
import com.dsms.booking.WaitingListEntry;
import com.dsms.schedule.ClassLevel;
import com.dsms.schedule.ClassSession;
import com.dsms.schedule.ClassStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class InstructorDtos {

    private InstructorDtos() {
    }

    public record CreateInstructorRequest(
            @NotNull Long userId,
            @NotBlank @Size(max = 255) String specialization,
            @Size(max = 4000) String description
    ) {
    }

    public record UpdateInstructorRequest(
            @NotBlank @Size(max = 255) String specialization,
            @Size(max = 4000) String description,
            boolean publicProfile
    ) {
    }

    public record InstructorResponse(
            Long id,
            Long userId,
            String firstName,
            String lastName,
            String email,
            String specialization,
            String description,
            boolean publicProfile
    ) {
        public static InstructorResponse from(InstructorProfile profile) {
            return new InstructorResponse(
                    profile.getId(),
                    profile.getUser().getId(),
                    profile.getUser().getFirstName(),
                    profile.getUser().getLastName(),
                    profile.getUser().getEmail(),
                    profile.getSpecialization(),
                    profile.getDescription(),
                    profile.isPublicProfile()
            );
        }
    }

    public record InstructorDashboardResponse(
            Long userId,
            Long instructorId,
            String firstName,
            String lastName,
            String specialization,
            String description,
            List<InstructorClassResponse> classes
    ) {
    }

    public record InstructorClassResponse(
            Long id,
            String title,
            String description,
            String danceStyle,
            ClassLevel level,
            Instant startAt,
            int durationMinutes,
            int capacity,
            int bookedPlaces,
            int availablePlaces,
            int waitlistCount,
            ClassStatus status,
            List<InstructorParticipantResponse> participants,
            List<InstructorWaitingListResponse> waitingList
    ) {
        public static InstructorClassResponse from(
                ClassSession session,
                List<InstructorParticipantResponse> participants,
                List<InstructorWaitingListResponse> waitingList
        ) {
            return new InstructorClassResponse(
                    session.getId(),
                    session.getTitle(),
                    session.getDescription(),
                    session.getDanceStyle(),
                    session.getLevel(),
                    session.getStartAt(),
                    session.getDurationMinutes(),
                    session.getCapacity(),
                    session.getBookedPlaces(),
                    Math.max(0, session.getCapacity() - session.getBookedPlaces()),
                    waitingList.size(),
                    session.getStatus(),
                    participants,
                    waitingList
            );
        }
    }

    public record InstructorParticipantResponse(
            Long reservationId,
            String firstName,
            String lastName,
            String email,
            String phone,
            String passName,
            ReservationStatus status,
            Instant bookedAt
    ) {
        public static InstructorParticipantResponse from(Reservation reservation) {
            return new InstructorParticipantResponse(
                    reservation.getId(),
                    reservation.getUser().getFirstName(),
                    reservation.getUser().getLastName(),
                    reservation.getUser().getEmail(),
                    reservation.getUser().getPhone(),
                    reservation.getUserPass() == null
                            ? null
                            : reservation.getUserPass().getPassType().getName(),
                    reservation.getStatus(),
                    reservation.getCreatedAt()
            );
        }
    }

    public record InstructorWaitingListResponse(
            Long id,
            String firstName,
            String lastName,
            String email,
            String phone,
            int position,
            Instant joinedAt
    ) {
        public static InstructorWaitingListResponse from(WaitingListEntry entry) {
            return new InstructorWaitingListResponse(
                    entry.getId(),
                    entry.getUser().getFirstName(),
                    entry.getUser().getLastName(),
                    entry.getUser().getEmail(),
                    entry.getUser().getPhone(),
                    entry.getPosition(),
                    entry.getCreatedAt()
            );
        }
    }
}
