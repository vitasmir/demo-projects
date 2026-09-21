package cz.listek.backend.transaction;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
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
@Table(name = "bank_transaction")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;

    @Column(nullable = false, length = 120)
    private String description;

    @Column(length = 34)
    private String counterpartyAccountNumber;

    @Column(length = 10)
    private String variableSymbol;

    @Column(length = 10)
    private String specificSymbol;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    protected Transaction() {
    }

    public Transaction(Account account, BigDecimal amount, TransactionType type, String description) {
        this(account, amount, type, description, null);
    }

    public Transaction(Account account, BigDecimal amount, TransactionType type, String description, String counterpartyAccountNumber) {
        this(account, amount, type, description, counterpartyAccountNumber, null, null);
    }

    public Transaction(Account account, BigDecimal amount, TransactionType type, String description,
            String counterpartyAccountNumber, String variableSymbol, String specificSymbol) {
        this.account = account;
        this.amount = amount;
        this.type = type;
        this.description = description;
        this.counterpartyAccountNumber = counterpartyAccountNumber;
        this.variableSymbol = variableSymbol;
        this.specificSymbol = specificSymbol;
        this.createdAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public Account getAccount() {
        return account;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public TransactionType getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public String getCounterpartyAccountNumber() {
        return counterpartyAccountNumber;
    }

    public String getVariableSymbol() {
        return variableSymbol;
    }

    public String getSpecificSymbol() {
        return specificSymbol;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
