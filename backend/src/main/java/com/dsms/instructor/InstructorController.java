package com.dsms.instructor;

import com.dsms.instructor.InstructorDtos.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class InstructorController {

    private final InstructorService service;

    public InstructorController(InstructorService service) {
        this.service = service;
    }

    @GetMapping("/instructors")
    public List<InstructorResponse> listPublic() {
        return service.listPublic();
    }

    @GetMapping("/admin/instructors")
    public List<InstructorResponse> listAll() {
        return service.listAll();
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

