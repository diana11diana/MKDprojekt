package com.dsms.instructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class InstructorDtos {

    private InstructorDtos() {
    }

    public record CreateInstructorRequest(
            @NotNull Long userId,
            @NotBlank @Size(max = 255) String specialization,
            @Size(max = 4000) String description
    ) {
    }

    public record UpdateInstructorRequest(
            @NotBlank @Size(max = 255) String specialization,
            @Size(max = 4000) String description,
            boolean publicProfile
    ) {
    }

    public record InstructorResponse(
            Long id,
            Long userId,
            String firstName,
            String lastName,
            String email,
            String specialization,
            String description,
            boolean publicProfile
    ) {
        public static InstructorResponse from(InstructorProfile profile) {
            return new InstructorResponse(
                    profile.getId(),
                    profile.getUser().getId(),
                    profile.getUser().getFirstName(),
                    profile.getUser().getLastName(),
                    profile.getUser().getEmail(),
                    profile.getSpecialization(),
                    profile.getDescription(),
                    profile.isPublicProfile()
            );
        }
    }
}

