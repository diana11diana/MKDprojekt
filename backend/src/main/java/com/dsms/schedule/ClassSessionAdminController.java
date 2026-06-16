package com.dsms.schedule;

import com.dsms.schedule.ClassSessionDtos.ClassSessionRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/classes")
public class ClassSessionAdminController {

    private final ClassSessionAdminService service;

    public ClassSessionAdminController(ClassSessionAdminService service) {
        this.service = service;
    }

    @GetMapping
    public List<ClassSessionResponse> listAll() {
        return service.listAll();
    }

    @PostMapping
    public ClassSessionResponse create(@Valid @RequestBody ClassSessionRequest request) {
        return service.create(request);
    }

    @PatchMapping("/{id}")
    public ClassSessionResponse update(
            @PathVariable Long id,
            @Valid @RequestBody ClassSessionRequest request
    ) {
        return service.update(id, request);
    }

    @PostMapping("/{id}/publish")
    public ClassSessionResponse publish(@PathVariable Long id) {
        return service.publish(id);
    }

    @PostMapping("/{id}/cancel")
    public ClassSessionResponse cancel(@PathVariable Long id) {
        return service.cancel(id);
    }
}

