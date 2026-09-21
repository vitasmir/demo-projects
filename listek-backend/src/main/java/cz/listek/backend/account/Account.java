package cz.listek.backend.account;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "bank_account")
public class Account {

    private static final BigDecimal DAYS_IN_YEAR = new BigDecimal("365");
    private static final BigDecimal PERCENT = new BigDecimal("100");

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 120)
    private String ownerName;
    @Column(length = 80)
    private String username;
    @Column(length = 80)
    private String firstName;
    @Column(length = 80)
    private String lastName;
    @Column(length = 11)
    private String birthNumber;
    @Column(nullable = false, length = 160)
    private String email;
    @Column(nullable = false, length = 240)
    private String address;
    @Column(nullable = false, length = 200)
    private String passwordHash;

    @Column(nullable = false, unique = true, length = 34)
    private String accountNumber;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 10)
    private AccountType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private CurrencyCode currency;

    protected Account() {
    }

    public Account(String ownerName, String accountNumber, BigDecimal balance, CurrencyCode currency) {
        this(ownerName, "", "", "", accountNumber, balance, currency);
    }

    public Account(String ownerName, String email, String address, String passwordHash, String accountNumber, BigDecimal balance, CurrencyCode currency) {
        this(ownerName, email, address, passwordHash, accountNumber, balance, currency, AccountType.CURRENT);
    }

    public Account(String ownerName, String email, String address, String passwordHash, String accountNumber, BigDecimal balance, CurrencyCode currency, AccountType type) {
        this(null, ownerName, null, null, null, email, address, passwordHash, accountNumber, balance, currency, type);
    }

    public Account(String username, String firstName, String lastName, String birthNumber, String email,
            String street, String city, String postalCode, String passwordHash, String accountNumber,
            BigDecimal balance, CurrencyCode currency) {
        this(username, firstName + " " + lastName, firstName, lastName, birthNumber, email,
                street + ", " + city + ", " + postalCode, passwordHash, accountNumber, balance, currency, AccountType.CURRENT);
    }

    private Account(String username, String ownerName, String firstName, String lastName, String birthNumber,
            String email, String address, String passwordHash, String accountNumber, BigDecimal balance,
            CurrencyCode currency, AccountType type) {
        this.ownerName = ownerName;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.birthNumber = birthNumber;
        this.email = email;
        this.address = address;
        this.passwordHash = passwordHash;
        this.accountNumber = accountNumber;
        this.balance = balance;
        this.currency = currency;
        this.type = type;
    }

    public UUID getId() {
        return id;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public String getUsername() {
        return username;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getBirthNumber() {
        return birthNumber;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getEmail() {
        return email;
    }

    public String getAddress() {
        return address;
    }

    String getPasswordHash() {
        return passwordHash;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public CurrencyCode getCurrency() {
        return currency;
    }

    public AccountType getType() {
        return type;
    }

    public void renameOwner(String ownerName) {
        this.ownerName = ownerName;
    }

    public void updateProfile(String ownerName, String email, String address, String passwordHash) {
        this.ownerName = ownerName;
        this.email = email;
        this.address = address;
        if (passwordHash != null) {
            this.passwordHash = passwordHash;
        }
    }

    public void debit(BigDecimal amount) {
        this.balance = this.balance.subtract(amount);
    }

    public void credit(BigDecimal amount) {
        this.balance = this.balance.add(amount);
    }

    public BigDecimal dailyOverdraftInterest(BigDecimal annualRate) {
        if (balance.signum() >= 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return balance.abs().multiply(annualRate)
                .divide(DAYS_IN_YEAR.multiply(PERCENT), 2, java.math.RoundingMode.HALF_UP);
    }
}
