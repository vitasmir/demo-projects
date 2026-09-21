package com.angular.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.messaging.simp.config.ChannelRegistration;

@Configuration
public class StompLoggingConfig implements WebSocketMessageBrokerConfigurer {

    private static final Logger log = LoggerFactory.getLogger(StompLoggingConfig.class);

    public StompLoggingConfig() {
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                try {
                    Object payload = message.getPayload();
                    MessageHeaderAccessor headers = MessageHeaderAccessor.getAccessor(message, MessageHeaderAccessor.class);
                    // Print directly so test runner captures it in stdout/stderr
                    System.out.println("[STOMP-INBOUND] headers=" + headers + " payloadType=" + (payload != null ? payload.getClass().getName() : "null") + " payload=" + payload);

                    String dest = headers != null ? (String) headers.getHeader("simpDestination") : null;
                    if (dest != null && dest.startsWith("/app/chat/")) {
                        System.out.println("[STOMP-INBOUND] intercepted chat destination=" + dest + " (processor disabled).");
                    }
                } catch (Throwable t) {
                    System.err.println("[STOMP-INBOUND] failed to log: " + t);
                }
                return message;
            }
        });
    }
}
