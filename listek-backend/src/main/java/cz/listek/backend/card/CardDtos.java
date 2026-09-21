package cz.listek.backend.card;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public final class CardDtos {

    private CardDtos() {
    }

    public record CardResponse(
            UUID id,
            UUID accountId,
            String holderName,
            String cardType,
            String lastFour,
            String expirationDate,
            boolean locked,
            BigDecimal paymentLimit,
            BigDecimal onlinePaymentLimit,
            BigDecimal withdrawalLimit,
            boolean onlinePayments,
            boolean inStorePayments,
            boolean cashWithdrawals) {

    }

    public record UpdateCardRequest(
            boolean locked,
            @NotNull
            @DecimalMin(value = "0.00") BigDecimal paymentLimit,
            @NotNull
            @DecimalMin(value = "0.00") BigDecimal onlinePaymentLimit,
            @NotNull
            @DecimalMin(value = "0.00") BigDecimal withdrawalLimit,
            boolean onlinePayments,
            boolean inStorePayments,
            boolean cashWithdrawals) {

    }
}
