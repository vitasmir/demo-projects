package com.angular.backend.config;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.broker.BrokerAvailabilityEvent;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class StompEventLogger {

    @EventListener
    public void onSessionConnected(SessionConnectedEvent ev) {
        StompHeaderAccessor sha = StompHeaderAccessor.wrap(ev.getMessage());
        System.out.println("[STOMP-EVENT] CONNECTED sessionId=" + sha.getSessionId() + " simpMessageType=" + sha.getMessageType());
    }

    @EventListener
    public void onSubscribe(SessionSubscribeEvent ev) {
        StompHeaderAccessor sha = StompHeaderAccessor.wrap(ev.getMessage());
        System.out.println("[STOMP-EVENT] SUBSCRIBE sessionId=" + sha.getSessionId() + " dest=" + sha.getDestination());
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent ev) {
        StompHeaderAccessor sha = StompHeaderAccessor.wrap(ev.getMessage());
        System.out.println("[STOMP-EVENT] DISCONNECT sessionId=" + sha.getSessionId() + " closeStatus=" + ev.getCloseStatus());
    }

    @EventListener
    public void onBrokerAvailable(BrokerAvailabilityEvent ev) {
        System.out.println("[STOMP-EVENT] BrokerAvailability: " + ev.isBrokerAvailable());
    }
}
