package cz.listek.backend.account;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.web.server.ResponseStatusException;

import cz.listek.backend.transaction.TransactionRepository;

class AccountServiceTest {

    private AccountRepository accountRepository;
    private TransactionRepository transactionRepository;
    private AccountService accountService;

    @BeforeEach
    void setUp() {
        accountRepository = mock(AccountRepository.class);
        transactionRepository = mock(TransactionRepository.class);
        accountService = new AccountService(accountRepository, transactionRepository);
    }

    @Test
    void transfersMoneyBetweenAccountsAndCreatesBothTransactions() {
        var source = new Account("Source User", "123456789", new BigDecimal("1000.00"), CurrencyCode.CZK);
        var target = new Account("Eva Kralova", "987654321", new BigDecimal("200.00"), CurrencyCode.CZK);
        var sourceId = UUID.randomUUID();
        var targetId = UUID.randomUUID();
        when(accountRepository.findById(sourceId)).thenReturn(Optional.of(source));
        when(accountRepository.findById(targetId)).thenReturn(Optional.of(target));

        accountService.transfer(sourceId, targetId, new BigDecimal("250.00"), "Test prevod");

        assertEquals(new BigDecimal("750.00"), source.getBalance());
        assertEquals(new BigDecimal("450.00"), target.getBalance());
        verify(transactionRepository, org.mockito.Mockito.times(2)).save(any());
    }

    @Test
    void rejectsTransferWhenBalanceIsTooLow() {
        var source = new Account("Source User", "123456789", new BigDecimal("100.00"), CurrencyCode.CZK);
        var target = new Account("Eva Kralova", "987654321", new BigDecimal("200.00"), CurrencyCode.CZK);
        var sourceId = UUID.randomUUID();
        var targetId = UUID.randomUUID();
        when(accountRepository.findById(sourceId)).thenReturn(Optional.of(source));
        when(accountRepository.findById(targetId)).thenReturn(Optional.of(target));

        assertThrows(ResponseStatusException.class, () -> accountService.transfer(sourceId, targetId, new BigDecimal("100.01"), "Test prevod"));

        assertEquals(new BigDecimal("100.00"), source.getBalance());
        assertEquals(new BigDecimal("200.00"), target.getBalance());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void rejectsRegistrationWithExistingEmail() {
        var request = new AccountDtos.CreateAccountRequest(
                "Source User", "jan@example.com", "Praha", "bezpecneheslo", "123456789", BigDecimal.ZERO, CurrencyCode.CZK);
        when(accountRepository.existsByEmailIgnoreCase("jan@example.com")).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> accountService.create(request));

        assertEquals(409, exception.getStatusCode().value());
        verify(accountRepository, never()).save(any());
    }

    @Test
    void registersAccountImmediately() {
        var request = new AccountDtos.RegisterAccountRequest(
                "jan.novak", "Jan", "Novak", "010101/1234", "jan.novak@example.com",
                "Hlavni 1", "Praha", "110 00", "bezpecneheslo");
        when(accountRepository.existsByUsernameIgnoreCaseAndType("jan.novak", AccountType.CURRENT)).thenReturn(false);
        when(accountRepository.existsByEmailIgnoreCase("jan.novak@example.com")).thenReturn(false);
        when(accountRepository.existsByAccountNumber(any())).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var account = accountService.register(request);

        assertEquals("jan.novak", account.username());
        assertEquals("jan.novak@example.com", account.email());
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void doesNotChargeDailyOverdraftInterestWhenAccountIsInCredit() {
        var account = new Account("Client", "123456789", new BigDecimal("100.00"), CurrencyCode.CZK);

        assertEquals(new BigDecimal("0.00"), account.dailyOverdraftInterest(new BigDecimal("12.900")));
    }

    @Test
    void calculatesDailyOverdraftInterestOnlyFromNegativeBalance() {
        var account = new Account("Client", "123456789", new BigDecimal("-1000.00"), CurrencyCode.CZK);

        assertEquals(new BigDecimal("0.35"), account.dailyOverdraftInterest(new BigDecimal("12.900")));
    }
}
