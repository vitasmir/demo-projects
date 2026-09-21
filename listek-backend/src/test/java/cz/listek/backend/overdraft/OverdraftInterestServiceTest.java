package cz.listek.backend.overdraft;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;

import cz.listek.backend.account.Account;
import cz.listek.backend.account.AccountRepository;
import cz.listek.backend.account.CurrencyCode;
import cz.listek.backend.settings.ProductInterestSettings;
import cz.listek.backend.settings.ProductInterestSettingsRepository;
import cz.listek.backend.transaction.TransactionRepository;

class OverdraftInterestServiceTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private OverdraftInterestAccrualRepository accrualRepository;
    @Mock
    private ProductInterestSettingsRepository interestSettingsRepository;
    @Mock
    private TransactionRepository transactionRepository;

    private OverdraftInterestService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new OverdraftInterestService(accountRepository, accrualRepository, interestSettingsRepository,
                transactionRepository, Clock.fixed(Instant.parse("2026-08-23T01:00:00Z"), ZoneId.of("Europe/Prague")));
    }

    @Test
    void chargesNegativeCurrentAccountUsingConfiguredRate() {
        var account = new Account("Client", "123456789", new BigDecimal("-1000.00"), CurrencyCode.CZK);
        var settings = org.mockito.Mockito.mock(ProductInterestSettings.class);
        when(settings.getOverdraftRate()).thenReturn(new BigDecimal("12.900"));
        when(interestSettingsRepository.findById(true)).thenReturn(Optional.of(settings));
        when(accountRepository.findAll()).thenReturn(List.of(account));
        when(accrualRepository.existsByAccountIdAndInterestDate(any(), any())).thenReturn(false);

        service.chargeDailyOverdraftInterest();

        org.junit.jupiter.api.Assertions.assertEquals(new BigDecimal("-1000.35"), account.getBalance());
        verify(accrualRepository).save(any());
        verify(transactionRepository).save(any());
    }

    @Test
    void skipsAccountInCredit() {
        var account = new Account("Client", "123456789", new BigDecimal("100.00"), CurrencyCode.CZK);
        var settings = org.mockito.Mockito.mock(ProductInterestSettings.class);
        when(settings.getOverdraftRate()).thenReturn(new BigDecimal("12.900"));
        when(interestSettingsRepository.findById(true)).thenReturn(Optional.of(settings));
        when(accountRepository.findAll()).thenReturn(List.of(account));

        service.chargeDailyOverdraftInterest();

        verify(accrualRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void doesNotChargeSameAccountAndDayTwice() {
        var account = new Account("Client", "123456789", new BigDecimal("-1000.00"), CurrencyCode.CZK);
        var settings = org.mockito.Mockito.mock(ProductInterestSettings.class);
        when(settings.getOverdraftRate()).thenReturn(new BigDecimal("12.900"));
        when(interestSettingsRepository.findById(true)).thenReturn(Optional.of(settings));
        when(accountRepository.findAll()).thenReturn(List.of(account));
        when(accrualRepository.existsByAccountIdAndInterestDate(any(), any())).thenReturn(false, true);

        service.chargeDailyOverdraftInterest();
        service.chargeDailyOverdraftInterest();

        verify(accrualRepository, times(1)).save(any());
        verify(transactionRepository, times(1)).save(any());
    }
}
