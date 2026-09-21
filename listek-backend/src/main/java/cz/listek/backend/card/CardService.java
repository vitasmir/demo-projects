package cz.listek.backend.card;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import cz.listek.backend.account.Account;
import cz.listek.backend.account.AccountRepository;
import cz.listek.backend.card.CardDtos.CardResponse;
import cz.listek.backend.card.CardDtos.UpdateCardRequest;

@Service
public class CardService {

    private final BankCardRepository cardRepository;
    private final AccountRepository accountRepository;

    public CardService(BankCardRepository cardRepository, AccountRepository accountRepository) {
        this.cardRepository = cardRepository;
        this.accountRepository = accountRepository;
    }

    @Transactional(readOnly = true)
    public List<CardResponse> findAll(UUID accountId) {
        requireAccount(accountId);
        return cardRepository.findByAccount_IdOrderByIdDesc(accountId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public CardResponse create(UUID accountId) {
        Account account = requireAccount(accountId);
        BankCard card = new BankCard(account, account.getOwnerName(), "Visa Classic", "2841", "08/29",
                new BigDecimal("50000.00"), new BigDecimal("30000.00"), new BigDecimal("10000.00"));
        return toResponse(cardRepository.save(card));
    }

    @Transactional
    public CardResponse update(UUID cardId, UpdateCardRequest request) {
        BankCard card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Karta nebyla nalezena"));
        card.updateSettings(request.locked(), request.paymentLimit(), request.onlinePaymentLimit(), request.withdrawalLimit(),
                request.onlinePayments(), request.inStorePayments(), request.cashWithdrawals());
        return toResponse(card);
    }

    private Account requireAccount(UUID id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ucet nebyl nalezen"));
    }

    private CardResponse toResponse(BankCard card) {
        return new CardResponse(card.getId(), card.getAccount().getId(), card.getHolderName(), card.getCardType(),
                card.getLastFour(), card.getExpirationDate(), card.isLocked(), card.getPaymentLimit(), card.getOnlinePaymentLimit(),
                card.getWithdrawalLimit(), card.isOnlinePayments(), card.isInStorePayments(), card.isCashWithdrawals());
    }
}
