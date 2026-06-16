package com.dsms.pass;

import com.dsms.pass.PassDtos.*;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class PassController {

    private final PassService service;

    public PassController(PassService service) {
        this.service = service;
    }

    @GetMapping("/pass-types")
    public List<PassTypeResponse> listPublicTypes() {
        return service.listPublicTypes();
    }

    @GetMapping("/me/passes")
    public List<UserPassResponse> listMyPasses(Authentication authentication) {
        return service.listMyPasses(authentication.getName());
    }

    @GetMapping("/admin/pass-types")
    public List<PassTypeResponse> listAllTypes() {
        return service.listAllTypes();
    }

    @PostMapping("/admin/pass-types")
    public PassTypeResponse createType(@Valid @RequestBody PassTypeRequest request) {
        return service.createType(request);
    }

    @PatchMapping("/admin/pass-types/{id}")
    public PassTypeResponse updateType(
            @PathVariable Long id,
            @Valid @RequestBody PassTypeRequest request
    ) {
        return service.updateType(id, request);
    }

    @PostMapping("/admin/user-passes")
    public UserPassResponse grantPass(@Valid @RequestBody GrantPassRequest request) {
        return service.grantPass(request);
    }
}

