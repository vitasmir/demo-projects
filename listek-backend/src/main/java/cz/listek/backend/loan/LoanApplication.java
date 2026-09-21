package cz.listek.backend.loan;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import cz.listek.backend.account.Account;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "loan_application")
public class LoanApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LoanType type;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private int repaymentMonths;

    @Column(nullable = false, precision = 7, scale = 4)
    private BigDecimal annualRate;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal monthlyPayment;

    @Column(nullable = false, length = 80)
    private String purpose;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LoanStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(length = 34)
    private String repaymentAccountNumber;

    @Column(length = 10)
    private String variableSymbol;

    @Column(length = 10)
    private String specificSymbol;

    private Integer repaymentDayOfMonth;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal repaidAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal remainingAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal principalBalance = BigDecimal.ZERO;

    private Integer remainingInstallments;

    private LocalDate dueDate;

    private Instant repaidAt;

    protected LoanApplication() {
    }

    public LoanApplication(Account account, LoanType type, BigDecimal amount, int repaymentMonths,
            BigDecimal annualRate, BigDecimal monthlyPayment, String purpose) {
        this.account = account;
        this.type = type;
        this.amount = amount;
        this.repaymentMonths = repaymentMonths;
        this.annualRate = annualRate;
        this.monthlyPayment = monthlyPayment;
        this.purpose = purpose;
        this.status = LoanStatus.PENDING;
        this.createdAt = Instant.now();
        this.remainingInstallments = repaymentMonths;
        this.principalBalance = amount;
        this.remainingAmount = calculateRemainingAmount();
    }

    public UUID getId() {
        return id;
    }

    public Account getAccount() {
        return account;
    }

    public LoanType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public int getRepaymentMonths() {
        return repaymentMonths;
    }

    public BigDecimal getAnnualRate() {
        return annualRate;
    }

    public BigDecimal getMonthlyPayment() {
        return monthlyPayment;
    }

    public String getPurpose() {
        return purpose;
    }

    public LoanStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getRepaymentAccountNumber() {
        return repaymentAccountNumber;
    }

    public String getVariableSymbol() {
        return variableSymbol;
    }

    public String getSpecificSymbol() {
        return specificSymbol;
    }

    public Integer getRepaymentDayOfMonth() {
        return repaymentDayOfMonth;
    }

    public BigDecimal getRepaidAmount() {
        return repaidAmount;
    }

    public BigDecimal getRemainingAmount() {
        return remainingAmount;
    }

    public Integer getRemainingInstallments() {
        return remainingInstallments;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public Instant getRepaidAt() {
        return repaidAt;
    }

    public BigDecimal calculateEarlyRepaymentAmount() {
        return this.principalBalance.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    public void configureRepayment(String repaymentAccountNumber, String variableSymbol, String specificSymbol,
            int repaymentDayOfMonth, LocalDate dueDate) {
        this.repaymentAccountNumber = repaymentAccountNumber;
        this.variableSymbol = variableSymbol;
        this.specificSymbol = specificSymbol;
        this.repaymentDayOfMonth = repaymentDayOfMonth;
        this.dueDate = dueDate;
        this.principalBalance = amount;
        this.remainingAmount = calculateRemainingAmount();
    }

    public void recordRepayment(BigDecimal amount, boolean earlyRepayment) {
        BigDecimal monthlyRate = annualRate.divide(new BigDecimal("1200"), 12, java.math.RoundingMode.HALF_UP);
        BigDecimal interest = principalBalance.multiply(monthlyRate)
                .setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal scheduledPayment = principalBalance.add(interest);
        if (amount.compareTo(earlyRepayment ? calculateEarlyRepaymentAmount() : scheduledPayment) >= 0) {
            this.repaidAmount = this.repaidAmount.add(amount);
            this.remainingAmount = BigDecimal.ZERO.setScale(2);
            this.remainingInstallments = 0;
            this.repaidAt = Instant.now();
            return;
        }
        this.repaidAmount = this.repaidAmount.add(amount);
        if (earlyRepayment) {
            this.principalBalance = this.principalBalance.subtract(amount).max(BigDecimal.ZERO);
            this.remainingInstallments = calculateRemainingInstallments(remainingInstallments);
        } else {
            int scheduledInstallments = remainingInstallments;
            BigDecimal unpaidAmount = amount;
            int paidInstallments = 0;
            BigDecimal balance = principalBalance;
            while (unpaidAmount.signum() > 0 && balance.signum() > 0
                    && paidInstallments < scheduledInstallments) {
                BigDecimal currentInterest = balance.multiply(monthlyRate)
                        .setScale(2, java.math.RoundingMode.HALF_UP);
                BigDecimal currentPayment = monthlyPayment.min(balance.add(currentInterest));
                if (unpaidAmount.compareTo(currentPayment) < 0) {
                    BigDecimal principalPayment = unpaidAmount.subtract(currentInterest).max(BigDecimal.ZERO);
                    balance = balance.subtract(principalPayment).max(BigDecimal.ZERO);
                    unpaidAmount = BigDecimal.ZERO;
                    break;
                }
                balance = balance.subtract(currentPayment.subtract(currentInterest)).max(BigDecimal.ZERO);
                unpaidAmount = unpaidAmount.subtract(currentPayment);
                paidInstallments++;
            }
            this.principalBalance = balance;
            this.remainingAmount = this.remainingAmount.subtract(amount).max(BigDecimal.ZERO);
            this.remainingInstallments = this.remainingAmount.signum() == 0
                    ? 0
                    : this.remainingAmount.divide(monthlyPayment, 0, java.math.RoundingMode.CEILING).intValue();
        }
        if (earlyRepayment) {
            this.remainingAmount = calculateRemainingAmount()
                    .setScale(2, java.math.RoundingMode.HALF_UP);
        }
    }

    private int calculateRemainingInstallments(int maximumInstallments) {
        if (principalBalance.signum() == 0) {
            return 0;
        }
        BigDecimal monthlyRate = annualRate.divide(new BigDecimal("1200"), 12, java.math.RoundingMode.HALF_UP);
        BigDecimal balance = principalBalance;
        int installments = 0;
        while (balance.signum() > 0 && installments < maximumInstallments) {
            BigDecimal interest = balance.multiply(monthlyRate).setScale(2, java.math.RoundingMode.HALF_UP);
            BigDecimal payment = monthlyPayment.min(balance.add(interest));
            balance = balance.subtract(payment.subtract(interest)).max(BigDecimal.ZERO);
            installments++;
        }
        return installments;
    }

    private BigDecimal calculateRemainingAmount() {
        if (principalBalance.signum() == 0 || remainingInstallments == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal monthlyRate = annualRate.divide(new BigDecimal("1200"), 12, java.math.RoundingMode.HALF_UP);
        BigDecimal balance = principalBalance;
        BigDecimal total = BigDecimal.ZERO;
        for (int month = 0; month < remainingInstallments && balance.signum() > 0; month++) {
            BigDecimal interest = balance.multiply(monthlyRate).setScale(2, java.math.RoundingMode.HALF_UP);
            BigDecimal payment = monthlyPayment.min(balance.add(interest));
            total = total.add(payment);
            balance = balance.subtract(payment.subtract(interest)).max(BigDecimal.ZERO);
        }
        return total.setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
