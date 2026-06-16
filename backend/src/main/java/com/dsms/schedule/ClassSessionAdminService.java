package com.dsms.schedule;

import com.dsms.auth.AuthException;
import com.dsms.instructor.InstructorProfile;
import com.dsms.instructor.InstructorService;
import com.dsms.schedule.ClassSessionDtos.ClassSessionRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ClassSessionAdminService {

    private final ClassSessionRepository repository;
    private final InstructorService instructorService;

    public ClassSessionAdminService(
            ClassSessionRepository repository,
            InstructorService instructorService
    ) {
        this.repository = repository;
        this.instructorService = instructorService;
    }

    @Transactional(readOnly = true)
    public List<ClassSessionResponse> listAll() {
        return repository.findAllByOrderByStartAtDesc()
                .stream()
                .map(ClassSessionResponse::from)
                .toList();
    }

    @Transactional
    public ClassSessionResponse create(ClassSessionRequest request) {
        InstructorProfile instructor = instructorService.getProfile(request.instructorId());
        ClassSession session = new ClassSession(
                request.title().trim(),
                normalizeDescription(request.description()),
                request.danceStyle().trim(),
                request.level(),
                instructor.getId(),
                instructorName(instructor),
                request.capacity(),
                request.startAt(),
                request.durationMinutes()
        );
        return ClassSessionResponse.from(repository.save(session));
    }

    @Transactional
    public ClassSessionResponse update(Long id, ClassSessionRequest request) {
        ClassSession session = getSession(id);
        if (session.getStatus() == ClassStatus.CANCELLED
                || session.getStatus() == ClassStatus.COMPLETED) {
            throw new AuthException(HttpStatus.CONFLICT, "This class can no longer be edited");
        }
        if (request.capacity() < session.getBookedPlaces()) {
            throw new AuthException(HttpStatus.CONFLICT, "Capacity is below current bookings");
        }

        InstructorProfile instructor = instructorService.getProfile(request.instructorId());
        session.update(
                request.title().trim(),
                normalizeDescription(request.description()),
                request.danceStyle().trim(),
                request.level(),
                instructor.getId(),
                instructorName(instructor),
                request.capacity(),
                request.startAt(),
                request.durationMinutes()
        );
        return ClassSessionResponse.from(session);
    }

    @Transactional
    public ClassSessionResponse publish(Long id) {
        ClassSession session = getSession(id);
        if (session.getStatus() != ClassStatus.DRAFT) {
            throw new AuthException(HttpStatus.CONFLICT, "Only draft classes can be published");
        }
        session.publish();
        return ClassSessionResponse.from(session);
    }

    @Transactional
    public ClassSessionResponse cancel(Long id) {
        ClassSession session = getSession(id);
        if (session.getStatus() == ClassStatus.CANCELLED
                || session.getStatus() == ClassStatus.COMPLETED) {
            throw new AuthException(HttpStatus.CONFLICT, "Class cannot be cancelled");
        }
        session.cancel();
        return ClassSessionResponse.from(session);
    }

    private ClassSession getSession(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "Class not found"));
    }

    private String instructorName(InstructorProfile profile) {
        return profile.getUser().getFirstName() + " " + profile.getUser().getLastName();
    }

    private String normalizeDescription(String description) {
        return description == null || description.isBlank() ? null : description.trim();
    }
}

