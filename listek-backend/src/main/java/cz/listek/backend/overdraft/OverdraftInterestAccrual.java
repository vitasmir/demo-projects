package cz.listek.backend.overdraft;

import java.math.BigDecimal;
import java.time.LocalDate;
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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "overdraft_interest_accrual", uniqueConstraints = @UniqueConstraint(name = "uq_overdraft_interest_account_date", columnNames = {"account_id", "interest_date"}))
public class OverdraftInterestAccrual {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "interest_date", nullable = false)
    private LocalDate interestDate;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    protected OverdraftInterestAccrual() {
    }

    public OverdraftInterestAccrual(Account account, LocalDate interestDate, BigDecimal amount) {
        this.account = account;
        this.interestDate = interestDate;
        this.amount = amount;
    }
}
