package cz.listek.backend.payment;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public final class PaymentDtos {

    private PaymentDtos() {
    }

    public record CreateStandingOrderRequest(@NotBlank String targetAccountNumber, @NotNull
            @DecimalMin("0.01") BigDecimal amount, @NotBlank String description, @Min(1)
            @Max(28) int dayOfMonth, @Pattern(regexp = "^$|\\d{1,10}") String variableSymbol,
            @Pattern(regexp = "^$|\\d{1,10}") String specificSymbol) {

    }

    public record UpdateStandingOrderRequest(@NotBlank String targetAccountNumber, @NotNull
            @DecimalMin("0.01") BigDecimal amount, @NotBlank String description, @Min(1)
            @Max(28) int dayOfMonth, @Pattern(regexp = "^$|\\d{1,10}") String variableSymbol,
            @Pattern(regexp = "^$|\\d{1,10}") String specificSymbol) {

    }

    public record StandingOrderResponse(UUID id, UUID accountId, String targetAccountNumber, BigDecimal amount, String description, int dayOfMonth, boolean active, String variableSymbol, String specificSymbol, OffsetDateTime createdAt) {

    }

    public record CreatePaymentTemplateRequest(@NotBlank String name, @NotBlank String targetAccountNumber, @NotNull
            @DecimalMin("0.01") BigDecimal amount, @NotBlank String description,
            @Pattern(regexp = "^$|\\d{1,10}") String variableSymbol,
            @Pattern(regexp = "^$|\\d{1,10}") String specificSymbol) {

    }

    public record UpdatePaymentTemplateRequest(@NotBlank String name, @NotBlank String targetAccountNumber, @NotNull
            @DecimalMin("0.01") BigDecimal amount, @NotBlank String description,
            @Pattern(regexp = "^$|\\d{1,10}") String variableSymbol,
            @Pattern(regexp = "^$|\\d{1,10}") String specificSymbol) {

    }

    public record PaymentTemplateResponse(UUID id, UUID accountId, String name, String targetAccountNumber, BigDecimal amount, String description, String variableSymbol, String specificSymbol, OffsetDateTime createdAt) {

    }
}
