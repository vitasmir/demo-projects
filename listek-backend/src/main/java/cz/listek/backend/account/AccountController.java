package cz.listek.backend.account;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import cz.listek.backend.account.AccountDtos.AccountResponse;
import cz.listek.backend.account.AccountDtos.CreateAccountRequest;
import cz.listek.backend.account.AccountDtos.UpdateAccountRequest;
import cz.listek.backend.transaction.TransactionDtos.TransactionResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public List<AccountResponse> findAll() {
        return accountService.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse create(@Valid @RequestBody CreateAccountRequest request) {
        return accountService.create(request);
    }

    @PostMapping("/{accountId}/savings")
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse createSavings(@PathVariable UUID accountId) {
        return accountService.createSavings(accountId);
    }

    @PatchMapping("/{accountId}")
    public AccountResponse update(@PathVariable UUID accountId, @Valid @RequestBody UpdateAccountRequest request) {
        return accountService.update(accountId, request);
    }

    @GetMapping("/{accountId}/transactions")
    public List<TransactionResponse> transactions(@PathVariable UUID accountId) {
        return accountService.transactions(accountId);
    }
}
