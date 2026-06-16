package com.dsms.user;

import com.dsms.auth.AuthDtos.UserResponse;
import com.dsms.user.UserManagementDtos.*;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class UserManagementController {

    private final UserManagementService service;

    public UserManagementController(UserManagementService service) {
        this.service = service;
    }

    @PatchMapping("/me")
    public UserResponse updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return service.updateProfile(
                authentication.getName(),
                request.firstName(),
                request.lastName(),
                request.phone()
        );
    }

    @GetMapping("/admin/users")
    public List<UserAdminResponse> listUsers() {
        return service.listUsers();
    }

    @PatchMapping("/admin/users/{id}/role")
    public UserAdminResponse changeRole(
            @PathVariable Long id,
            @Valid @RequestBody ChangeRoleRequest request,
            Authentication authentication
    ) {
        return service.changeRole(id, request.role(), authentication.getName());
    }

    @PatchMapping("/admin/users/{id}/status")
    public UserAdminResponse changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody ChangeStatusRequest request,
            Authentication authentication
    ) {
        return service.changeStatus(id, request.status(), authentication.getName());
    }
}

