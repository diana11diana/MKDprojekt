package com.dsms.instructor;

import com.dsms.instructor.InstructorDtos.*;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class InstructorController {

    private final InstructorService service;
    private final InstructorClassService instructorClassService;

    public InstructorController(
            InstructorService service,
            InstructorClassService instructorClassService
    ) {
        this.service = service;
        this.instructorClassService = instructorClassService;
    }

    @GetMapping("/instructors")
    public List<InstructorResponse> listPublic() {
        return service.listPublic();
    }

    @GetMapping("/admin/instructors")
    public List<InstructorResponse> listAll() {
        return service.listAll();
    }

    @GetMapping("/instructor/dashboard")
    public InstructorDashboardResponse dashboard(Authentication authentication) {
        return service.dashboard(authentication.getName());
    }

    @PostMapping("/instructor/classes/{classId}/start")
    public InstructorClassResponse confirmStart(
            @PathVariable Long classId,
            Authentication authentication
    ) {
        return instructorClassService.confirmStart(classId, authentication.getName());
    }

    @PatchMapping("/instructor/classes/{classId}/reservations/{reservationId}")
    public InstructorClassResponse updateAttendance(
            @PathVariable Long classId,
            @PathVariable Long reservationId,
            @Valid @RequestBody UpdateAttendanceRequest request,
            Authentication authentication
    ) {
        return instructorClassService.updateAttendance(
                classId,
                reservationId,
                authentication.getName(),
                request
        );
    }

    @GetMapping("/admin/instructors/{id}/dashboard")
    public InstructorDashboardResponse adminDashboard(@PathVariable Long id) {
        return service.dashboardForAdmin(id);
    }

    @PostMapping("/admin/instructors")
    public InstructorResponse create(@Valid @RequestBody CreateInstructorRequest request) {
        return service.create(request);
    }

    @PatchMapping("/admin/instructors/{id}")
    public InstructorResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateInstructorRequest request
    ) {
        return service.update(id, request);
    }
}
