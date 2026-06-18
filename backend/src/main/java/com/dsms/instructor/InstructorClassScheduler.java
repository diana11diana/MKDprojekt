package com.dsms.instructor;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class InstructorClassScheduler {

    private final InstructorClassService instructorClassService;

    public InstructorClassScheduler(InstructorClassService instructorClassService) {
        this.instructorClassService = instructorClassService;
    }

    @Scheduled(fixedDelay = 15000)
    public void autoCancelUnconfirmedClasses() {
        instructorClassService.autoCancelUnconfirmedClasses();
    }
}
