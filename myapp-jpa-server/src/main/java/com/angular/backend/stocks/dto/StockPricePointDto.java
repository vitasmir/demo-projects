package com.angular.backend.stocks.dto;

public record StockPricePointDto(
        long timestamp,
        double price
) {
}
