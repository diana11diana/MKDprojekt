package com.dsms.pass;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public final class PassDtos {

    private PassDtos() {
    }

    public record PassTypeRequest(
            @NotBlank @Size(max = 100) String name,
            @Size(max = 4000) String description,
            @NotNull PassTypeKind type,
            @Min(1) Integer visitCount,
            @Min(1) int validityDays,
            @NotNull @DecimalMin("0.00") BigDecimal price,
            boolean active
    ) {
    }

    public record GrantPassRequest(
            @NotNull Long userId,
            @NotNull Long passTypeId,
            Instant validFrom
    ) {
    }

    public record CreatePassOrderRequest(
            @NotNull Long passTypeId
    ) {
    }

    public record CompletePaymentRequest(
            @Size(max = 100) String paymentReference
    ) {
    }

    public record PassTypeResponse(
            Long id,
            String name,
            String description,
            PassTypeKind type,
            Integer visitCount,
            int validityDays,
            BigDecimal price,
            String currency,
            boolean active
    ) {
        public static PassTypeResponse from(PassType passType) {
            return new PassTypeResponse(
                    passType.getId(),
                    passType.getName(),
                    passType.getDescription(),
                    passType.getType(),
                    passType.getVisitCount(),
                    passType.getValidityDays(),
                    passType.getPrice(),
                    passType.getCurrency(),
                    passType.isActive()
            );
        }
    }

    public record UserPassResponse(
            Long id,
            Long userId,
            String userName,
            Long passTypeId,
            String passName,
            PassTypeKind type,
            UserPassStatus status,
            Integer remainingVisits,
            Instant validFrom,
            Instant validUntil
    ) {
        public static UserPassResponse from(UserPass pass) {
            return new UserPassResponse(
                    pass.getId(),
                    pass.getUser().getId(),
                    pass.getUser().getFirstName() + " " + pass.getUser().getLastName(),
                    pass.getPassType().getId(),
                    pass.getPassType().getName(),
                    pass.getPassType().getType(),
                    pass.getStatus(),
                    pass.getRemainingVisits(),
                    pass.getValidFrom(),
                    pass.getValidUntil()
            );
        }
    }

    public record PassOrderResponse(
            Long id,
            Long userId,
            String userName,
            Long passTypeId,
            String passName,
            PassOrderStatus status,
            java.math.BigDecimal amount,
            String currency,
            String paymentReference,
            Long userPassId,
            Instant paidAt,
            Instant cancelledAt,
            Instant createdAt
    ) {
        public static PassOrderResponse from(PassOrder order) {
            return new PassOrderResponse(
                    order.getId(),
                    order.getUser().getId(),
                    order.getUser().getFirstName() + " " + order.getUser().getLastName(),
                    order.getPassType().getId(),
                    order.getPassType().getName(),
                    order.getStatus(),
                    order.getAmount(),
                    order.getCurrency(),
                    order.getPaymentReference(),
                    order.getUserPass() == null ? null : order.getUserPass().getId(),
                    order.getPaidAt(),
                    order.getCancelledAt(),
                    order.getCreatedAt()
            );
        }
    }
}
