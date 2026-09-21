package com.angular.backend.broker;

import com.angular.backend.broker.dto.OrderRequestDto;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/broker")
public class BrokerController {

    private final BrokerService brokerService;

    public BrokerController(BrokerService brokerService) {
        this.brokerService = brokerService;
    }

    @GetMapping("/{symbol}/asks")
    public List<TradeOrder> getAsks(@PathVariable String symbol) {
        return brokerService.getAsks(symbol);
    }

    @GetMapping("/{symbol}/bids")
    public List<TradeOrder> getBids(@PathVariable String symbol) {
        return brokerService.getBids(symbol);
    }

    @PostMapping("/order")
    public TradeOrder placeOrder(@RequestBody OrderRequestDto request) {
        return brokerService.placeOrder(request);
    }
}