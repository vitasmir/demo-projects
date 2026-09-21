package cz.listek.backend.card;

import java.math.BigDecimal;
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
@Table(name = "bank_card")
public class BankCard {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(nullable = false, length = 120)
    private String holderName;

    @Column(nullable = false, length = 40)
    private String cardType;

    @Column(nullable = false, length = 4)
    private String lastFour;

    @Column(nullable = false, length = 5)
    private String expirationDate;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal paymentLimit;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal onlinePaymentLimit;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal withdrawalLimit;

    @Column(nullable = false)
    private boolean locked;

    @Column(nullable = false)
    private boolean onlinePayments;

    @Column(nullable = false)
    private boolean inStorePayments;

    @Column(nullable = false)
    private boolean cashWithdrawals;

    protected BankCard() {
    }

    public BankCard(Account account, String holderName, String cardType, String lastFour, String expirationDate,
            BigDecimal paymentLimit, BigDecimal onlinePaymentLimit, BigDecimal withdrawalLimit) {
        this.account = account;
        this.holderName = holderName;
        this.cardType = cardType;
        this.lastFour = lastFour;
        this.expirationDate = expirationDate;
        this.paymentLimit = paymentLimit;
        this.onlinePaymentLimit = onlinePaymentLimit;
        this.withdrawalLimit = withdrawalLimit;
        this.onlinePayments = true;
        this.inStorePayments = true;
        this.cashWithdrawals = true;
    }

    public UUID getId() {
        return id;
    }

    public Account getAccount() {
        return account;
    }

    public String getHolderName() {
        return holderName;
    }

    public String getCardType() {
        return cardType;
    }

    public String getLastFour() {
        return lastFour;
    }

    public String getExpirationDate() {
        return expirationDate;
    }

    public BigDecimal getPaymentLimit() {
        return paymentLimit;
    }

    public BigDecimal getOnlinePaymentLimit() {
        return onlinePaymentLimit;
    }

    public BigDecimal getWithdrawalLimit() {
        return withdrawalLimit;
    }

    public boolean isLocked() {
        return locked;
    }

    public boolean isOnlinePayments() {
        return onlinePayments;
    }

    public boolean isInStorePayments() {
        return inStorePayments;
    }

    public boolean isCashWithdrawals() {
        return cashWithdrawals;
    }

    public void updateSettings(boolean locked, BigDecimal paymentLimit, BigDecimal onlinePaymentLimit, BigDecimal withdrawalLimit,
            boolean onlinePayments, boolean inStorePayments, boolean cashWithdrawals) {
        this.locked = locked;
        this.paymentLimit = paymentLimit;
        this.onlinePaymentLimit = onlinePaymentLimit;
        this.withdrawalLimit = withdrawalLimit;
        this.onlinePayments = onlinePayments;
        this.inStorePayments = inStorePayments;
        this.cashWithdrawals = cashWithdrawals;
    }
}
