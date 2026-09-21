package cz.listek.backend.loan;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class LoanDtos {

    private LoanDtos() {
    }

    public record CreateLoanApplicationRequest(
            @NotNull LoanType type,
            @NotNull
            @DecimalMin(value = "20000.00") BigDecimal amount,
            @Min(12)
            @Max(360) int repaymentMonths,
            @NotBlank
            @Size(max = 80) String purpose) {

    }

    public record LoanApplicationResponse(
            UUID id,
            LoanType type,
            BigDecimal amount,
            int repaymentMonths,
            BigDecimal annualRate,
            BigDecimal monthlyPayment,
            String purpose,
            LoanStatus status,
            Instant createdAt,
            String repaymentAccountNumber,
            String variableSymbol,
            String specificSymbol,
            Integer repaymentDayOfMonth,
            BigDecimal repaidAmount,
            BigDecimal remainingAmount,
            Integer remainingInstallments,
            BigDecimal earlyRepaymentAmount,
            LocalDate dueDate) {

    }
}
