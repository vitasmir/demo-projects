package com.angular.backend.employees;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class LogController {

    private static final Logger logger = LoggerFactory.getLogger(LogController.class);

    @MessageMapping("/send-log")
    @SendTo("/topic/logs")
    public String handleLogMessage(String message) {
        logger.info("Received log message via WebSocket: {}", message);
        // You can process the message here (e.g., save to a database)
        // The return value is broadcast to all clients subscribed to "/topic/logs"
        return message;
    }
}
