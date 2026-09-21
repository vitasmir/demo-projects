package cz.listek.backend.overdraft;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cz.listek.backend.account.Account;
import cz.listek.backend.account.AccountRepository;
import cz.listek.backend.account.AccountType;
import cz.listek.backend.settings.ProductInterestSettingsRepository;
import cz.listek.backend.transaction.Transaction;
import cz.listek.backend.transaction.TransactionRepository;
import cz.listek.backend.transaction.TransactionType;

@Service
public class OverdraftInterestService {

    private static final ZoneId BANK_ZONE = ZoneId.of("Europe/Prague");

    private final AccountRepository accountRepository;
    private final OverdraftInterestAccrualRepository accrualRepository;
    private final ProductInterestSettingsRepository interestSettingsRepository;
    private final TransactionRepository transactionRepository;
    private final Clock clock;

    @Autowired
    public OverdraftInterestService(AccountRepository accountRepository,
            OverdraftInterestAccrualRepository accrualRepository,
            ProductInterestSettingsRepository interestSettingsRepository,
            TransactionRepository transactionRepository) {
        this(accountRepository, accrualRepository, interestSettingsRepository, transactionRepository, Clock.system(BANK_ZONE));
    }

    OverdraftInterestService(AccountRepository accountRepository,
            OverdraftInterestAccrualRepository accrualRepository,
            ProductInterestSettingsRepository interestSettingsRepository,
            TransactionRepository transactionRepository,
            Clock clock) {
        this.accountRepository = accountRepository;
        this.accrualRepository = accrualRepository;
        this.interestSettingsRepository = interestSettingsRepository;
        this.transactionRepository = transactionRepository;
        this.clock = clock;
    }

    @Scheduled(cron = "0 0 1 * * *", zone = "Europe/Prague")
    @Transactional
    public void chargeDailyOverdraftInterest() {
        LocalDate interestDate = LocalDate.now(clock).minusDays(1);
        var annualRate = interestSettingsRepository.findById(true)
                .map(settings -> settings.getOverdraftRate())
                .orElse(null);
        if (annualRate == null) {
            return;
        }

        accountRepository.findAll().stream()
                .filter(account -> account.getType() == AccountType.CURRENT)
                .filter(account -> account.getBalance().signum() < 0)
                .forEach(account -> charge(account, interestDate, annualRate));
    }

    private void charge(Account account, LocalDate interestDate, java.math.BigDecimal annualRate) {
        if (accrualRepository.existsByAccountIdAndInterestDate(account.getId(), interestDate)) {
            return;
        }
        var interest = account.dailyOverdraftInterest(annualRate);
        if (interest.signum() == 0) {
            return;
        }
        account.debit(interest);
        accrualRepository.save(new OverdraftInterestAccrual(account, interestDate, interest));
        transactionRepository.save(new Transaction(account, interest.negate(), TransactionType.DEBIT,
                "Denni urok kontokorentu za " + interestDate));
    }
}
