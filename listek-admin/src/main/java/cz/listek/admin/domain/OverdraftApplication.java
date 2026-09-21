package cz.listek.admin.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

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
@Table(name = "overdraft_application")
public class OverdraftApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private AdminAccount account;

    @Column(nullable = false)
    private BigDecimal requestedLimit;

    @Column(nullable = false)
    private BigDecimal monthlyIncome;

    @Column(precision = 7, scale = 4)
    private BigDecimal annualRate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant decidedAt;
    private String decisionNote;

    protected OverdraftApplication() {
    }

    public OverdraftApplication(AdminAccount account, BigDecimal requestedLimit, BigDecimal monthlyIncome) {
        this.account = account;
        this.requestedLimit = requestedLimit;
        this.monthlyIncome = monthlyIncome;
        this.status = ApplicationStatus.PENDING;
        this.createdAt = Instant.now();
    }

    public OverdraftApplication(AdminAccount account, BigDecimal requestedLimit, BigDecimal monthlyIncome, BigDecimal annualRate) {
        this(account, requestedLimit, monthlyIncome);
        this.annualRate = annualRate;
    }

    public void decide(ApplicationStatus status, String note) {
        this.status = status;
        this.decisionNote = note;
        this.decidedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public AdminAccount getAccount() {
        return account;
    }

    public BigDecimal getRequestedLimit() {
        return requestedLimit;
    }

    public BigDecimal getMonthlyIncome() {
        return monthlyIncome;
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

    public String getDecisionNote() {
        return decisionNote;
    }

    public BigDecimal getAnnualRate() {
        return annualRate;
    }
}
