package com.angular.backend.employees;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class RabbitMQConfig {

    public static final String EMPLOYEE_EVENTS_QUEUE = "employee-events-queue";

    // A constant for the exchange name
    public static final String TOPIC_EXCHANGE_NAME = "app-topic-exchange";

    // A routing key to link the exchange and queue
    public static final String ROUTING_KEY_NEW = "employee.new";

    // A new routing key for updated employees
    public static final String ROUTING_KEY_UPDATED = "employee.updated";

    public static final String ROUTING_KEY_DELETED = "employee.deleted";

    /**
     * Defines the queue. A queue is a buffer that stores messages.
     *
     * @return A new Queue bean.
     */
    @Bean
    Queue employeeEventsQueue() {
        return new Queue(EMPLOYEE_EVENTS_QUEUE, true);
    }

    /**
     * Defines the exchange. An exchange receives messages from producers and
     * pushes them to queues. A TopicExchange is powerful for routing messages
     * based on pattern matching.
     *
     * @return A new TopicExchange bean.
     */
    @Bean
    TopicExchange exchange() {
        return new TopicExchange(TOPIC_EXCHANGE_NAME);
    }

    /**
     * Defines the binding between the queue and the exchange. This tells the
     * exchange to send messages with a specific routing key to our queue.
     *
     * @param newEmployeeQueue The queue bean.
     * @param exchange The exchange bean.
     * @return A new Binding bean.
     */
    @Bean
    Binding bindingNew(Queue employeeEventsQueue, TopicExchange exchange) {
        return BindingBuilder.bind(employeeEventsQueue).to(exchange).with(ROUTING_KEY_NEW);
    }

    /**
     * Defines the binding for the updated employee routing key on the shared
     * employee events queue.
     *
     * @param employeeEventsQueue The shared employee events queue.
     * @param exchange The exchange bean.
     * @return A new Binding bean.
     */
    @Bean
    Binding bindingUpdated(Queue employeeEventsQueue, TopicExchange exchange) {
        return BindingBuilder.bind(employeeEventsQueue).to(exchange).with(ROUTING_KEY_UPDATED);
    }

    @Bean
    Binding bindingDeleted(Queue employeeEventsQueue, TopicExchange exchange) {
        return BindingBuilder.bind(employeeEventsQueue).to(exchange).with(ROUTING_KEY_DELETED);
    }

    /**
     * Configures a message converter to use JSON for serializing and
     * deserializing messages. This allows sending and receiving POJOs. We pass
     * the existing ObjectMapper to ensure consistent serialization (e.g., for
     * LocalDate).
     *
     * @param objectMapper The Spring-managed ObjectMapper.
     * @return A Jackson2JsonMessageConverter bean.
     */
    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(final ConnectionFactory connectionFactory,
            final Jackson2JsonMessageConverter jsonMessageConverter) {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter);
        return rabbitTemplate;
    }
}
