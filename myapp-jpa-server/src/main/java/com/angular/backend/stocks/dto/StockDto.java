
package com.angular.backend.stocks.dto;

public record StockDto(
        String symbol,
        String name
        ) {

}

class Test {

    public static StockDto createStock(String symbol, String name) {
        StockDto stockDto = new StockDto(symbol, name);
        System.out.println("Created stock: " 
        + stockDto.name() + "=" + stockDto.symbol());
        return stockDto;
    }
    public static void main(String[] args) {
        StockDto stockAPPL = createStock("AAPL", "Apple Inc.");
        StockDto stockGoogle = createStock("GOOGL", "Alphabet Inc.");

        System.out.println("Stock AAPL: " + stockAPPL.name() + "=" + stockAPPL.symbol());
        System.out.println("Stock GOOGL: " + stockGoogle.name() + "=" + stockGoogle.symbol());
    }

}
