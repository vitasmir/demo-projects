package com.angular.backend.broker.dto;

import com.angular.backend.broker.TradeOrder;

public class OrderRequestDto {
    private String symbol;
    private TradeOrder.OrderType orderType;
    private Double price;
    private Integer quantity;

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public TradeOrder.OrderType getOrderType() { return orderType; }
    public void setOrderType(TradeOrder.OrderType orderType) { this.orderType = orderType; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}