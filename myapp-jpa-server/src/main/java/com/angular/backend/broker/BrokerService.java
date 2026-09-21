package com.angular.backend.broker;

import com.angular.backend.broker.dto.OrderRequestDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BrokerService {

    private final TradeOrderRepository repository;

    public BrokerService(TradeOrderRepository repository) {
        this.repository = repository;
    }

    public List<TradeOrder> getAsks(String symbol) {
        return repository.findBySymbolAndStatusAndOrderTypeOrderByPriceAsc(symbol, TradeOrder.OrderStatus.OPEN, TradeOrder.OrderType.SELL);
    }

    public List<TradeOrder> getBids(String symbol) {
        return repository.findBySymbolAndStatusAndOrderTypeOrderByPriceDesc(symbol, TradeOrder.OrderStatus.OPEN, TradeOrder.OrderType.BUY);
    }

    @Transactional
    public TradeOrder placeOrder(OrderRequestDto request) {
        TradeOrder order = new TradeOrder();
        order.setSymbol(request.getSymbol());
        order.setOrderType(request.getOrderType());
        order.setPrice(request.getPrice());
        order.setQuantity(request.getQuantity());
        order.setStatus(TradeOrder.OrderStatus.OPEN);
        order.setCreatedAt(LocalDateTime.now());
        
        repository.save(order);
        matchOrders(request.getSymbol());
        return order;
    }
    
    private void matchOrders(String symbol) {
        List<TradeOrder> buys = repository.findBySymbolAndStatusAndOrderTypeOrderByPriceDesc(symbol, TradeOrder.OrderStatus.OPEN, TradeOrder.OrderType.BUY);
        List<TradeOrder> sells = repository.findBySymbolAndStatusAndOrderTypeOrderByPriceAsc(symbol, TradeOrder.OrderStatus.OPEN, TradeOrder.OrderType.SELL);
        
        for (TradeOrder buy : buys) {
            for (TradeOrder sell : sells) {
                if (sell.getStatus() != TradeOrder.OrderStatus.OPEN) continue;
                if (buy.getStatus() != TradeOrder.OrderStatus.OPEN) break;
                
                if (buy.getPrice() >= sell.getPrice()) {
                    int tradeQty = Math.min(buy.getQuantity(), sell.getQuantity());
                    buy.setQuantity(buy.getQuantity() - tradeQty);
                    sell.setQuantity(sell.getQuantity() - tradeQty);
                    
                    if (buy.getQuantity() == 0) buy.setStatus(TradeOrder.OrderStatus.FULFILLED);
                    if (sell.getQuantity() == 0) sell.setStatus(TradeOrder.OrderStatus.FULFILLED);
                    
                    repository.save(buy);
                    repository.save(sell);
                }
            }
        }
    }
}