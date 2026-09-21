package cz.listek.backend.account;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import cz.listek.backend.account.AccountDtos.AccountResponse;
import cz.listek.backend.account.AccountDtos.CreateAccountRequest;
import cz.listek.backend.account.AccountDtos.RegisterAccountRequest;
import cz.listek.backend.account.AccountDtos.UpdateAccountRequest;
import cz.listek.backend.loan.LoanApplicationRepository;
import cz.listek.backend.transaction.Transaction;
import cz.listek.backend.transaction.TransactionDtos.TransactionResponse;
import cz.listek.backend.transaction.TransactionRepository;
import cz.listek.backend.transaction.TransactionType;

@Service
public class AccountService {

    private static final SecureRandom ACCOUNT_NUMBER_RANDOM = new SecureRandom();

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final LoanApplicationRepository loanApplicationRepository;

    AccountService(AccountRepository accountRepository, TransactionRepository transactionRepository) {
        this(accountRepository, transactionRepository, null);
    }

    @Autowired
    public AccountService(AccountRepository accountRepository, TransactionRepository transactionRepository,
            LoanApplicationRepository loanApplicationRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.loanApplicationRepository = loanApplicationRepository;
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> findAll() {
        return accountRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public AccountResponse create(CreateAccountRequest request) {
        if (accountRepository.existsByEmailIgnoreCase(request.email().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail je jiz zaregistrovan");
        }
        var initialBalance = request.initialBalance() == null ? BigDecimal.ZERO : request.initialBalance();
        if (initialBalance.signum() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pocatecni zustatek nesmi byt zaporny");
        }
        var currency = request.currency() == null ? CurrencyCode.CZK : request.currency();
        return toResponse(accountRepository.save(new Account(request.ownerName().trim(), request.email().trim(), request.address().trim(), PasswordHasher.hash(request.password()), request.accountNumber(), initialBalance, currency)));
    }

    @Transactional
    public AccountResponse createSavings(UUID currentAccountId) {
        Account currentAccount = requireAccount(currentAccountId);
        if (accountRepository.existsByEmailIgnoreCaseAndType(currentAccount.getEmail(), AccountType.SAVINGS)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Sporici ucet uz existuje");
        }
        String accountNumber;
        do {
            accountNumber = String.format("%09d", ACCOUNT_NUMBER_RANDOM.nextInt(1_000_000_000));
        } while (accountRepository.existsByAccountNumber(accountNumber));
        Account savingsAccount = new Account(currentAccount.getOwnerName(), currentAccount.getEmail(), currentAccount.getAddress(),
                currentAccount.getPasswordHash(), accountNumber, BigDecimal.ZERO, currentAccount.getCurrency(), AccountType.SAVINGS);
        return toResponse(accountRepository.save(savingsAccount));
    }

    @Transactional
    public AccountResponse register(RegisterAccountRequest request) {
        if (accountRepository.existsByUsernameIgnoreCaseAndType(request.username().trim(), AccountType.CURRENT)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Uživatelské jméno je již registrované");
        }
        if (accountRepository.existsByEmailIgnoreCase(request.email().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail je již zaregistrovaný");
        }
        String accountNumber;
        do {
            accountNumber = String.format("%09d", ACCOUNT_NUMBER_RANDOM.nextInt(1_000_000_000));
        } while (accountRepository.existsByAccountNumber(accountNumber));
        Account account = new Account(request.username().trim(), request.firstName().trim(), request.lastName().trim(),
                request.birthNumber().trim(), request.email().trim(), request.street().trim(), request.city().trim(),
                request.postalCode().trim(), PasswordHasher.hash(request.password()), accountNumber, BigDecimal.ZERO, CurrencyCode.CZK);
        return toResponse(accountRepository.save(account));
    }

    @Transactional
    public AccountResponse update(UUID accountId, UpdateAccountRequest request) {
        Account account = requireAccount(accountId);
        account.updateProfile(request.ownerName().trim(), request.email().trim(), request.address().trim(), request.password() == null || request.password().isBlank() ? null : PasswordHasher.hash(request.password()));
        return toResponse(account);
    }

    @Transactional(readOnly = true)
    public AccountResponse login(String username, String password) {
        Account account = accountRepository.findByUsernameIgnoreCaseAndType(username.trim(), AccountType.CURRENT)
                .filter(candidate -> PasswordHasher.matches(password, candidate.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Nesprávné uživatelské jméno nebo heslo"));
        return toResponse(account);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> transactions(UUID accountId) {
        requireAccount(accountId);
        return transactionRepository.findByAccountIdOrderByCreatedAtDesc(accountId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public void transfer(UUID fromId, UUID toId, BigDecimal amount, String description) {
        var source = requireAccount(fromId);
        var target = requireAccount(toId);
        transferBetweenAccounts(source, target, amount, description);
    }

    @Transactional
    public void transfer(UUID fromId, String toAccountNumber, BigDecimal amount, String description) {
        transfer(fromId, toAccountNumber, amount, description, null, null);
    }

    @Transactional
    public void transfer(UUID fromId, String toAccountNumber, BigDecimal amount, String description,
            String variableSymbol, String specificSymbol) {
        var source = requireAccount(fromId);
        var target = accountRepository.findByAccountNumber(toAccountNumber.trim()).orElse(null);
        if (target != null && source.getId().equals(target.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Zdrojovy a cilovy ucet musi byt rozdilne");
        }
        if (target == null) {
            if (source.getBalance().compareTo(amount) < 0) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Na uctu neni dostatecny zustatek");
            }
            source.debit(amount);
            transactionRepository.save(new Transaction(source, amount.negate(), TransactionType.DEBIT, description, toAccountNumber.trim(), variableSymbol, specificSymbol));
            if (loanApplicationRepository != null) {
                loanApplicationRepository.findByAccount_IdAndRepaymentAccountNumberAndVariableSymbolAndSpecificSymbol(
                        source.getId(), toAccountNumber.trim(), variableSymbol, specificSymbol)
                        .ifPresent(loan -> loan.recordRepayment(amount, description.contains("Mimořádná")));
            }
            return;
        }
        transferBetweenAccounts(source, target, amount, description);
    }

    private void transferBetweenAccounts(Account source, Account target, BigDecimal amount, String description) {
        if (source.getId() != null && source.getId().equals(target.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Zdrojovy a cilovy ucet musi byt rozdilne");
        }
        if (source.getCurrency() != target.getCurrency()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Prevod mezi ruznymi menami neni podporovan");
        }
        if (source.getBalance().compareTo(amount) < 0) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Na uctu neni dostatecny zustatek");
        }
        source.debit(amount);
        target.credit(amount);
        transactionRepository.save(new Transaction(source, amount.negate(), TransactionType.DEBIT, description, target.getAccountNumber()));
        transactionRepository.save(new Transaction(target, amount, TransactionType.CREDIT, description, source.getAccountNumber()));
    }

    private Account requireAccount(UUID id) {
        return accountRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ucet nebyl nalezen"));
    }

    private AccountResponse toResponse(Account account) {
        return new AccountResponse(account.getId(), account.getOwnerName(), account.getUsername(), account.getFirstName(), account.getLastName(),
                account.getBirthNumber(), account.getEmail(), account.getAddress(), account.getAccountNumber(), account.getBalance(), account.getCurrency(), account.getType());
    }

    private TransactionResponse toResponse(Transaction transaction) {
        return new TransactionResponse(transaction.getId(), transaction.getAccount().getId(), transaction.getAmount(), transaction.getType(), transaction.getDescription(), transaction.getCounterpartyAccountNumber(), transaction.getVariableSymbol(), transaction.getSpecificSymbol(), transaction.getCreatedAt());
    }
}
