package com.example.json;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class JsonLoggingApp {
    private static final Logger logger = LoggerFactory.getLogger(JsonLoggingApp.class);

    public static void main(String[] args) {
        // 1. Simple logging
        logger.info("Application started successfully");

        try {
            // 2. Logging with MDC (Mapped Diagnostic Context)
            // These fields will be automatically added as JSON keys!
            MDC.put("userId", "12345");
            MDC.put("transactionId", "TX-987");
            
            logger.info("Processing payment for premium subscription");

            // Simulate an error
            throw new RuntimeException("Database connection timed out");

        } catch (Exception e) {
            // 3. Exception logging (Stack traces become a JSON field)
            logger.error("A critical error occurred while processing transaction", e);
        } finally {
            MDC.clear();
        }
    }
}

