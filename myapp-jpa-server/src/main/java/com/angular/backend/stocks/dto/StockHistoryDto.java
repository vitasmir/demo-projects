package com.angular.backend.stocks.dto;

import java.util.List;

public record StockHistoryDto(
        String symbol,
        String period,
        List<StockPricePointDto> points
) {
}
