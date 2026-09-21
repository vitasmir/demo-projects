package com.angular.backend.stocks;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpSubscription;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.angular.backend.stocks.dto.StockHistoryDto;

@Service
public class BrokerHistoryStreamService {

    private static final Logger log = LoggerFactory.getLogger(BrokerHistoryStreamService.class);
    private static final Pattern DESTINATION_PATTERN = Pattern.compile("^/topic/broker/history/([^/]+)/([^/]+)$");

    private final SimpUserRegistry simpUserRegistry;
    private final SimpMessagingTemplate messagingTemplate;
    private final NasdaqStocksService stocksService;
    private final Map<String, Long> lastSentAt = new ConcurrentHashMap<>();

    public BrokerHistoryStreamService(
            SimpUserRegistry simpUserRegistry,
            SimpMessagingTemplate messagingTemplate,
            NasdaqStocksService stocksService) {
        this.simpUserRegistry = simpUserRegistry;
        this.messagingTemplate = messagingTemplate;
        this.stocksService = stocksService;
    }

    @Scheduled(fixedDelay = 3000)
    public void pushHistoryUpdates() {
        long now = Instant.now().toEpochMilli();
        Set<String> activeDestinations = simpUserRegistry.getUsers().stream()
                .flatMap(this::subscriptionsForUser)
                .map(SimpSubscription::getDestination)
                .filter(destination -> destination != null && DESTINATION_PATTERN.matcher(destination).matches())
                .collect(java.util.stream.Collectors.toSet());

        lastSentAt.keySet().removeIf(destination -> !activeDestinations.contains(destination));

        for (String destination : activeDestinations) {
            Matcher matcher = DESTINATION_PATTERN.matcher(destination);
            if (!matcher.matches()) {
                continue;
            }

            String period = matcher.group(1);
            String symbol = matcher.group(2);
            long refreshIntervalMs = getRefreshIntervalMs(period);
            long lastSent = lastSentAt.getOrDefault(destination, 0L);
            if (now - lastSent < refreshIntervalMs) {
                continue;
            }

            try {
                StockHistoryDto history = stocksService.getHistory(symbol, period);
                messagingTemplate.convertAndSend(destination, history);
                lastSentAt.put(destination, now);
            } catch (Exception ex) {
                log.debug("Skipping live broker update for {} {}: {}", symbol, period, ex.getMessage());
            }
        }
    }

    private java.util.stream.Stream<SimpSubscription> subscriptionsForUser(SimpUser user) {
        return user.getSessions().stream()
                .flatMap(session -> session.getSubscriptions().stream());
    }

    private long getRefreshIntervalMs(String period) {
        return switch (period) {
            case "1h" -> 3000L;
            case "1d" -> 5000L;
            case "3m" -> 15000L;
            case "1y" -> 30000L;
            case "max" -> 60000L;
            default -> 5000L;
        };
    }
}
