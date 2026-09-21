package com.angular.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${spring.websocket.broker-relay.enabled:false}")
    private boolean brokerRelayEnabled;

    @Value("${spring.rabbitmq.host}")
    private String relayHost;

    @Value("${spring.rabbitmq.stomp.port:61613}")
    private int relayPort;

    @Value("${spring.rabbitmq.stomp.username}")
    private String clientLogin;

    @Value("${spring.rabbitmq.stomp.password}")
    private String clientPasscode;

    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(1);
        taskScheduler.setThreadNamePrefix("wss-heartbeat-thread-");
        return taskScheduler;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // These are prefixes for messages that are bound for @MessageMapping-annotated methods.
        // A client would send a message to a destination like "/app/send-log".
        config.setApplicationDestinationPrefixes("/app");

        // Use the STOMP broker relay.
        // This configures a broker relay that connects to an external STOMP broker (RabbitMQ).
        // It will forward messages to the broker for destinations prefixed with "/topic" or "/group".
        long startTime = System.currentTimeMillis();
        // Use embedded simple broker by default. Relay is opt-in via
        // spring.websocket.broker-relay.enabled=true.
        if (!brokerRelayEnabled || relayHost == null || relayHost.trim().isEmpty()) {
            // Embedded simple broker is sufficient for single-node setups and local development.
            config.enableSimpleBroker("/topic", "/group", "/rooms", "/queue");
            long endTime = System.currentTimeMillis();
            String reason = !brokerRelayEnabled
                    ? "broker relay disabled"
                    : "no RabbitMQ host configured";
            System.out.println("Configured embedded simple STOMP broker in " + (endTime - startTime) + "ms (" + reason + ")");
        } else {
            // Allow broker relay to handle '/topic', '/group', '/rooms' and '/queue' destinations
            // Spring's STOMP relay may strip the '/topic' prefix before forwarding to the broker
            // (so the broker sees destinations like '/rooms/NAME'). Registering '/rooms' and
            // '/queue' here ensures the relay treats those destinations as relayable.
            config.enableStompBrokerRelay("/topic", "/group", "/rooms", "/queue")
                    .setRelayHost(relayHost)
                    .setRelayPort(relayPort)
                    .setClientLogin(clientLogin)
                    .setClientPasscode(clientPasscode)
                    .setSystemLogin(clientLogin)
                    .setSystemPasscode(clientPasscode)
                    .setTaskScheduler(taskScheduler())
                    .setSystemHeartbeatSendInterval(5000)
                    .setSystemHeartbeatReceiveInterval(5000);
            long endTime = System.currentTimeMillis();
            System.out.println("STOMP broker relay configuration took " + (endTime - startTime) + "ms");
        }
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // This registers the "/ws" endpoint with SockJS fallback for broad browser compatibility.
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS()
                .setWebSocketEnabled(true);

        // Native WebSocket endpoint: makes '/ws2' available for direct WebSocket clients (production-ready clients may use this)
        registry.addEndpoint("/ws2")
                .setAllowedOriginPatterns("*");

        

    }
}