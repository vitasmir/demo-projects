package cz.listek.backend.settings;

import java.math.BigDecimal;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "product_interest_settings")
public class ProductInterestSettings {

    @Id
    private boolean id;
    private BigDecimal savingsRate;
    private BigDecimal overdraftRate;
    private BigDecimal personalLoanRate;
    private BigDecimal homeLoanRate;
    private BigDecimal mortgageRate;
    private BigDecimal mortgageMinimumEquityPercent;

    protected ProductInterestSettings() {
    }

    public BigDecimal getSavingsRate() {
        return savingsRate;
    }

    public BigDecimal getOverdraftRate() {
        return overdraftRate;
    }

    public BigDecimal getPersonalLoanRate() {
        return personalLoanRate;
    }

    public BigDecimal getHomeLoanRate() {
        return homeLoanRate;
    }

    public BigDecimal getMortgageRate() {
        return mortgageRate;
    }

    public BigDecimal getMortgageMinimumEquityPercent() {
        return mortgageMinimumEquityPercent;
    }
}
