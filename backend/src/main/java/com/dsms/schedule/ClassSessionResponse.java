package com.dsms.schedule;

import java.time.Instant;

public record ClassSessionResponse(
        Long id,
        String title,
        String description,
        String danceStyle,
        ClassLevel level,
        Long instructorId,
        String instructorName,
        Instant startAt,
        int durationMinutes,
        int capacity,
        int availablePlaces,
        ClassStatus status
) {
    static ClassSessionResponse from(ClassSession session) {
        return new ClassSessionResponse(
                session.getId(),
                session.getTitle(),
                session.getDescription(),
                session.getDanceStyle(),
                session.getLevel(),
                session.getInstructorId(),
                session.getInstructorName(),
                session.getStartAt(),
                session.getDurationMinutes(),
                session.getCapacity(),
                Math.max(0, session.getCapacity() - session.getBookedPlaces()),
                session.getStatus()
        );
    }
}
