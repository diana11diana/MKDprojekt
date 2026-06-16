package com.dsms.auth;

import com.dsms.user.User;
import com.dsms.user.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Size(max = 100) String firstName,
            @NotBlank @Size(max = 100) String lastName,
            @NotBlank @Email @Size(max = 254) String email,
            @Size(max = 32) String phone,
            @NotBlank
            @Size(min = 8, max = 72)
            @Pattern(
                    regexp = "^(?=.*[A-ZА-Я])(?=.*\\d).+$",
                    message = "Password must contain an uppercase letter and a digit"
            )
            String password
    ) {
    }

    public record RegisterResponse(
            String message,
            String verificationToken
    ) {
    }

    public record VerifyEmailRequest(@NotBlank String token) {
    }

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
    ) {
    }

    public record AuthResponse(
            String accessToken,
            long expiresInSeconds,
            UserResponse user
    ) {
    }

    public record UserResponse(
            Long id,
            String firstName,
            String lastName,
            String email,
            String phone,
            UserRole role
    ) {
        public static UserResponse from(User user) {
            return new UserResponse(
                    user.getId(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getEmail(),
                    user.getPhone(),
                    user.getRole()
            );
        }
    }
}

