package cz.listek.backend.transaction;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public final class TransactionDtos {

    private TransactionDtos() {
    }

    public record TransferRequest(
            @NotNull UUID fromAccountId,
            @NotBlank String toAccountNumber,
            @NotNull
            @DecimalMin("0.01") BigDecimal amount,
            @NotBlank String description,
            @Pattern(regexp = "^$|\\d{1,10}") String variableSymbol,
            @Pattern(regexp = "^$|\\d{1,10}") String specificSymbol) {

    }

    public record TransactionResponse(UUID id, UUID accountId, BigDecimal amount, TransactionType type, String description, String counterpartyAccountNumber, String variableSymbol, String specificSymbol, OffsetDateTime createdAt) {

    }
}
