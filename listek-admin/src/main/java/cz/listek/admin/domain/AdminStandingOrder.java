package cz.listek.admin.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

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
public class AdminStandingOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private AdminAccount account;

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

    protected AdminStandingOrder() {
    }

    public AdminStandingOrder(AdminAccount account, String targetAccountNumber, BigDecimal amount,
            String description, int dayOfMonth) {
        this(account, targetAccountNumber, amount, description, dayOfMonth, null, null);
    }

    public AdminStandingOrder(AdminAccount account, String targetAccountNumber, BigDecimal amount,
            String description, int dayOfMonth, String variableSymbol, String specificSymbol) {
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
}
