package cz.listek.admin.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "loan_application")
public class AdminLoanApplication {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private AdminAccount account;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private int repaymentMonths;

    @Column(nullable = false)
    private BigDecimal annualRate;

    @Column(nullable = false)
    private BigDecimal monthlyPayment;

    @Column(nullable = false)
    private String purpose;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant decidedAt;
    @Column(length = 80)
    private String approvedBy;
    private Instant approvedAt;
    private Instant repaidAt;
    private String decisionNote;
    @Column(nullable = false)
    private int mortgageApprovalCount;
    @Column(length = 80)
    private String firstMortgageApprover;

    @Column(length = 34)
    private String repaymentAccountNumber;
    @Column(length = 10)
    private String variableSymbol;
    @Column(length = 10)
    private String specificSymbol;
    private Integer repaymentDayOfMonth;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal repaidAmount;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal remainingAmount;
    private Integer remainingInstallments;
    private LocalDate dueDate;

    protected AdminLoanApplication() {
    }

    public void decide(ApplicationStatus status, String note, String approver) {
        this.status = status;
        this.decisionNote = note;
        this.decidedAt = Instant.now();
        if (status == ApplicationStatus.APPROVED) {
            this.approvedBy = approver;
            this.approvedAt = this.decidedAt;
        }
    }

    public void recordMortgageApproval(String approver) {
        this.mortgageApprovalCount++;
        if (firstMortgageApprover == null) {
            firstMortgageApprover = approver;
        }
        this.decisionNote = "Čeká na druhé schválení hypotéky";
    }

    public UUID getId() {
        return id;
    }

    public AdminAccount getAccount() {
        return account;
    }

    public String getType() {
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

    public ApplicationStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public String getDecisionNote() {
        return decisionNote;
    }

    public int getMortgageApprovalCount() {
        return mortgageApprovalCount;
    }

    public String getFirstMortgageApprover() {
        return firstMortgageApprover;
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

    public void configureRepayment(String repaymentAccountNumber, String variableSymbol, String specificSymbol,
            int repaymentDayOfMonth, LocalDate dueDate) {
        this.repaymentAccountNumber = repaymentAccountNumber;
        this.variableSymbol = variableSymbol;
        this.specificSymbol = specificSymbol;
        this.repaymentDayOfMonth = repaymentDayOfMonth;
        this.dueDate = dueDate;
        this.repaidAmount = BigDecimal.ZERO;
        this.remainingAmount = monthlyPayment.multiply(BigDecimal.valueOf(repaymentMonths));
        this.remainingInstallments = repaymentMonths;
    }
}
