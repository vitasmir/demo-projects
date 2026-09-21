package cz.listek.backend.payment;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import cz.listek.backend.payment.PaymentDtos.CreatePaymentTemplateRequest;
import cz.listek.backend.payment.PaymentDtos.CreateStandingOrderRequest;
import cz.listek.backend.payment.PaymentDtos.PaymentTemplateResponse;
import cz.listek.backend.payment.PaymentDtos.StandingOrderResponse;
import cz.listek.backend.payment.PaymentDtos.UpdatePaymentTemplateRequest;
import cz.listek.backend.payment.PaymentDtos.UpdateStandingOrderRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/accounts/{accountId}/standing-orders")
    public List<StandingOrderResponse> standingOrders(@PathVariable UUID accountId) {
        return paymentService.standingOrders(accountId);
    }

    @PostMapping("/accounts/{accountId}/standing-orders")
    @ResponseStatus(HttpStatus.CREATED)
    public StandingOrderResponse createStandingOrder(@PathVariable UUID accountId, @Valid @RequestBody CreateStandingOrderRequest request) {
        return paymentService.createStandingOrder(accountId, request);
    }

    @PatchMapping("/standing-orders/{orderId}")
    public StandingOrderResponse updateStandingOrder(@PathVariable UUID orderId, @Valid @RequestBody UpdateStandingOrderRequest request) {
        return paymentService.updateStandingOrder(orderId, request);
    }

    @DeleteMapping("/standing-orders/{orderId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteStandingOrder(@PathVariable UUID orderId) {
        paymentService.deleteStandingOrder(orderId);
    }

    @GetMapping("/accounts/{accountId}/payment-templates")
    public List<PaymentTemplateResponse> templates(@PathVariable UUID accountId) {
        return paymentService.templates(accountId);
    }

    @PostMapping("/accounts/{accountId}/payment-templates")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentTemplateResponse createTemplate(@PathVariable UUID accountId, @Valid @RequestBody CreatePaymentTemplateRequest request) {
        return paymentService.createTemplate(accountId, request);
    }

    @PatchMapping("/payment-templates/{templateId}")
    public PaymentTemplateResponse updateTemplate(@PathVariable UUID templateId, @Valid @RequestBody UpdatePaymentTemplateRequest request) {
        return paymentService.updateTemplate(templateId, request);
    }

    @DeleteMapping("/payment-templates/{templateId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTemplate(@PathVariable UUID templateId) {
        paymentService.deleteTemplate(templateId);
    }
}
