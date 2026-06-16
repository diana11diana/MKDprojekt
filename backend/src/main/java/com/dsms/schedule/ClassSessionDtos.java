package com.dsms.schedule;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class ClassSessionDtos {

    private ClassSessionDtos() {
    }

    public record ClassSessionRequest(
            @NotBlank @Size(max = 150) String title,
            @Size(max = 4000) String description,
            @NotBlank @Size(max = 100) String danceStyle,
            @NotNull ClassLevel level,
            @NotNull Long instructorId,
            @Min(1) @Max(500) int capacity,
            @NotNull @Future Instant startAt,
            @Min(15) @Max(480) int durationMinutes
    ) {
    }
}

