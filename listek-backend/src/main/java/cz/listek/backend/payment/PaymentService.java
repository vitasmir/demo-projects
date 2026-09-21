package cz.listek.backend.payment;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import cz.listek.backend.account.Account;
import cz.listek.backend.account.AccountRepository;
import cz.listek.backend.payment.PaymentDtos.CreatePaymentTemplateRequest;
import cz.listek.backend.payment.PaymentDtos.CreateStandingOrderRequest;
import cz.listek.backend.payment.PaymentDtos.PaymentTemplateResponse;
import cz.listek.backend.payment.PaymentDtos.StandingOrderResponse;
import cz.listek.backend.payment.PaymentDtos.UpdatePaymentTemplateRequest;
import cz.listek.backend.payment.PaymentDtos.UpdateStandingOrderRequest;

@Service
public class PaymentService {

    private final AccountRepository accountRepository;
    private final StandingOrderRepository standingOrderRepository;
    private final PaymentTemplateRepository paymentTemplateRepository;

    public PaymentService(AccountRepository accountRepository, StandingOrderRepository standingOrderRepository, PaymentTemplateRepository paymentTemplateRepository) {
        this.accountRepository = accountRepository;
        this.standingOrderRepository = standingOrderRepository;
        this.paymentTemplateRepository = paymentTemplateRepository;
    }

    @Transactional(readOnly = true)
    public List<StandingOrderResponse> standingOrders(UUID accountId) {
        requireAccount(accountId);
        return standingOrderRepository.findByAccount_IdOrderByCreatedAtDesc(accountId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public StandingOrderResponse createStandingOrder(UUID accountId, CreateStandingOrderRequest request) {
        Account account = requireAccount(accountId);
        return toResponse(standingOrderRepository.save(new StandingOrder(account, request.targetAccountNumber().trim(), request.amount(), request.description().trim(), request.dayOfMonth(), request.variableSymbol(), request.specificSymbol())));
    }

    @Transactional
    public StandingOrderResponse updateStandingOrder(UUID orderId, UpdateStandingOrderRequest request) {
        StandingOrder order = standingOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trvalý příkaz nebyl nalezen"));
        order.update(request.targetAccountNumber().trim(), request.amount(), request.description().trim(), request.dayOfMonth(), request.variableSymbol(), request.specificSymbol());
        return toResponse(order);
    }

    @Transactional
    public void deleteStandingOrder(UUID orderId) {
        if (!standingOrderRepository.existsById(orderId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Trvalý příkaz nebyl nalezen");
        }
        standingOrderRepository.deleteById(orderId);
    }

    @Transactional(readOnly = true)
    public List<PaymentTemplateResponse> templates(UUID accountId) {
        requireAccount(accountId);
        return paymentTemplateRepository.findByAccount_IdOrderByCreatedAtDesc(accountId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public PaymentTemplateResponse createTemplate(UUID accountId, CreatePaymentTemplateRequest request) {
        Account account = requireAccount(accountId);
        return toResponse(paymentTemplateRepository.save(new PaymentTemplate(account, request.name().trim(), request.targetAccountNumber().trim(), request.amount(), request.description().trim(), request.variableSymbol(), request.specificSymbol())));
    }

    @Transactional
    public PaymentTemplateResponse updateTemplate(UUID templateId, UpdatePaymentTemplateRequest request) {
        PaymentTemplate template = paymentTemplateRepository.findById(templateId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sablona nebyla nalezena"));
        template.update(request.name().trim(), request.targetAccountNumber().trim(), request.amount(), request.description().trim(), request.variableSymbol(), request.specificSymbol());
        return toResponse(template);
    }

    @Transactional
    public void deleteTemplate(UUID templateId) {
        if (!paymentTemplateRepository.existsById(templateId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Sablona nebyla nalezena");

        }
        paymentTemplateRepository.deleteById(templateId);
    }

    private Account requireAccount(UUID id) {
        return accountRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ucet nebyl nalezen"));
    }

    private StandingOrderResponse toResponse(StandingOrder order) {
        return new StandingOrderResponse(order.getId(), order.getAccount().getId(), order.getTargetAccountNumber(), order.getAmount(), order.getDescription(), order.getDayOfMonth(), order.isActive(), order.getVariableSymbol(), order.getSpecificSymbol(), order.getCreatedAt());
    }

    private PaymentTemplateResponse toResponse(PaymentTemplate template) {
        return new PaymentTemplateResponse(template.getId(), template.getAccount().getId(), template.getName(), template.getTargetAccountNumber(), template.getAmount(), template.getDescription(), template.getVariableSymbol(), template.getSpecificSymbol(), template.getCreatedAt());
    }
}
