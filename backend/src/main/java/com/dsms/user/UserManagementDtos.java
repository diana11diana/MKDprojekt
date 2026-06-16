package com.dsms.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class UserManagementDtos {

    private UserManagementDtos() {
    }

    public record UpdateProfileRequest(
            @NotBlank @Size(max = 100) String firstName,
            @NotBlank @Size(max = 100) String lastName,
            @Size(max = 32) String phone
    ) {
    }

    public record ChangeRoleRequest(@NotNull UserRole role) {
    }

    public record ChangeStatusRequest(@NotNull UserStatus status) {
    }

    public record UserAdminResponse(
            Long id,
            String firstName,
            String lastName,
            String email,
            String phone,
            UserRole role,
            UserStatus status,
            Instant createdAt
    ) {
        public static UserAdminResponse from(User user) {
            return new UserAdminResponse(
                    user.getId(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getEmail(),
                    user.getPhone(),
                    user.getRole(),
                    user.getStatus(),
                    user.getCreatedAt()
            );
        }
    }
}

