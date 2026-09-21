package cz.listek.backend.card;

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

import cz.listek.backend.card.CardDtos.CardResponse;
import cz.listek.backend.card.CardDtos.UpdateCardRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @GetMapping("/accounts/{accountId}/cards")
    public List<CardResponse> findAll(@PathVariable UUID accountId) {
        return cardService.findAll(accountId);
    }

    @PostMapping("/accounts/{accountId}/cards")
    @ResponseStatus(HttpStatus.CREATED)
    public CardResponse create(@PathVariable UUID accountId) {
        return cardService.create(accountId);
    }

    @PatchMapping("/cards/{cardId}")
    public CardResponse update(@PathVariable UUID cardId, @Valid @RequestBody UpdateCardRequest request) {
        return cardService.update(cardId, request);
    }
}
