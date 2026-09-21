package com.angular.backend.chat;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import com.angular.backend.chat.dto.IncomingChatMessage;

import jakarta.validation.Valid;

@Controller
public class ChatWebSocketController {

    private final ChatService chatService;

    public ChatWebSocketController(ChatService chatService) {
        this.chatService = chatService;
    }

    @MessageMapping("/chat.send")
    public void sendMessageToGroup(@Valid IncomingChatMessage payload) {
        chatService.saveAndBroadcast(payload);
    }
}
