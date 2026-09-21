package cz.listek.admin.domain;

import java.math.BigDecimal;
import java.time.Instant;
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
@Table(name = "bank_transaction")
public class AdminTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private AdminAccount account;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 20)
    private String type;

    @Column(nullable = false, length = 120)
    private String description;

    @Column(nullable = false)
    private Instant createdAt;

    protected AdminTransaction() {
    }

    public AdminTransaction(AdminAccount account, BigDecimal amount, String type, String description) {
        this.account = account;
        this.amount = amount;
        this.type = type;
        this.description = description;
        this.createdAt = Instant.now();
    }
}
