package com.angular.backend.broker;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TradeOrderRepository extends JpaRepository<TradeOrder, Long> {
    List<TradeOrder> findBySymbolAndStatusAndOrderTypeOrderByPriceAsc(String symbol, TradeOrder.OrderStatus status, TradeOrder.OrderType orderType);
    List<TradeOrder> findBySymbolAndStatusAndOrderTypeOrderByPriceDesc(String symbol, TradeOrder.OrderStatus status, TradeOrder.OrderType orderType);
}