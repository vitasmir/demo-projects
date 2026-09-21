package com.angular.backend.stocks;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.angular.backend.stocks.dto.StockDto;
import com.angular.backend.stocks.dto.StockHistoryDto;

@RestController
@RequestMapping("/api/stocks")
public class NasdaqStocksController {

    private final NasdaqStocksService stocksService;

    public NasdaqStocksController(NasdaqStocksService stocksService) {
        this.stocksService = stocksService;
    }

    @GetMapping("/nasdaq")
    public List<StockDto> listNasdaqStocks() {
        return stocksService.listNasdaqStocks();
    }

    @GetMapping("/{symbol}/history")
    public StockHistoryDto getHistory(@PathVariable String symbol, @RequestParam(defaultValue = "1d") String period) {
        return stocksService.getHistory(symbol, period);
    }
}
