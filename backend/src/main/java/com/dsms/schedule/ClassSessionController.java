package com.dsms.schedule;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/classes")
public class ClassSessionController {

    private final ClassSessionRepository repository;

    public ClassSessionController(ClassSessionRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<ClassSessionResponse> getSchedule() {
        return repository
                .findByStatusAndStartAtAfterOrderByStartAtAsc(
                        ClassStatus.PUBLISHED,
                        Instant.now()
                )
                .stream()
                .map(ClassSessionResponse::from)
                .toList();
    }
}

