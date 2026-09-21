package com.angular.backend.employees;

import java.nio.charset.StandardCharsets;

import jakarta.jms.BytesMessage;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;

import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
public class EmployeeJmsListener {

    @JmsListener(destination = RabbitMQConfig.EMPLOYEE_EVENTS_QUEUE, containerFactory = "employeeJmsListenerContainerFactory")
    public void onEmployeeEvent(Message message) throws JMSException {
        printEvent("employee event", message);
    }

    private void printEvent(String eventType, Message message) throws JMSException {
        String body;
        if (message instanceof TextMessage textMessage) {
            body = textMessage.getText();
        } else if (message instanceof BytesMessage bytesMessage) {
            body = readBytesMessage(bytesMessage);
        } else {
            body = message.toString();
        }
        System.out.println("[JMS] " + eventType + " " + body);
    }

    private String readBytesMessage(BytesMessage message) throws JMSException {
        byte[] body = new byte[(int) message.getBodyLength()];
        message.readBytes(body);
        return new String(body, StandardCharsets.UTF_8);
    }
}