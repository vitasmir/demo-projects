package cz.listek.backend.payment;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import cz.listek.backend.account.Account;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "standing_order")
public class StandingOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;
    @Column(nullable = false, length = 34)
    private String targetAccountNumber;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    @Column(nullable = false, length = 120)
    private String description;
    @Column(length = 10)
    private String variableSymbol;
    @Column(length = 10)
    private String specificSymbol;
    @Column(nullable = false)
    private int dayOfMonth;
    @Column(nullable = false)
    private boolean active;
    @Column(nullable = false)
    private OffsetDateTime createdAt;

    protected StandingOrder() {
    }

    public StandingOrder(Account account, String targetAccountNumber, BigDecimal amount, String description, int dayOfMonth) {
        this(account, targetAccountNumber, amount, description, dayOfMonth, null, null);
    }

    public StandingOrder(Account account, String targetAccountNumber, BigDecimal amount, String description, int dayOfMonth, String variableSymbol, String specificSymbol) {
        this.account = account;
        this.targetAccountNumber = targetAccountNumber;
        this.amount = amount;
        this.description = description;
        this.variableSymbol = variableSymbol;
        this.specificSymbol = specificSymbol;
        this.dayOfMonth = dayOfMonth;
        this.active = true;
        this.createdAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public Account getAccount() {
        return account;
    }

    public String getTargetAccountNumber() {
        return targetAccountNumber;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    public String getVariableSymbol() {
        return variableSymbol;
    }

    public String getSpecificSymbol() {
        return specificSymbol;
    }

    public int getDayOfMonth() {
        return dayOfMonth;
    }

    public boolean isActive() {
        return active;
    }

    public void update(String targetAccountNumber, BigDecimal amount, String description, int dayOfMonth) {
        update(targetAccountNumber, amount, description, dayOfMonth, null, null);
    }

    public void update(String targetAccountNumber, BigDecimal amount, String description, int dayOfMonth, String variableSymbol, String specificSymbol) {
        this.targetAccountNumber = targetAccountNumber;
        this.amount = amount;
        this.description = description;
        this.dayOfMonth = dayOfMonth;
        this.variableSymbol = variableSymbol;
        this.specificSymbol = specificSymbol;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
