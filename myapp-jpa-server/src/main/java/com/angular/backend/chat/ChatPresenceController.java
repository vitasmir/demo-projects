package com.angular.backend.chat;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.angular.backend.chat.dto.ChatUserDto;
import com.angular.backend.chat.dto.ChatUserLoginRequest;
import com.angular.backend.chat.dto.ChatUserLogoutRequest;

import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/chat/presence")
public class ChatPresenceController {

    private final ChatPresenceService chatPresenceService;

    public ChatPresenceController(ChatPresenceService chatPresenceService) {
        this.chatPresenceService = chatPresenceService;
    }

    @GetMapping("/users/{groupId}")
    public List<ChatUserDto> listOnlineUsers(@PathVariable String groupId) {
        return chatPresenceService.listOnlineUsers(groupId);
    }

    @PostMapping("/login")
    public ChatUserDto login(@Valid @RequestBody ChatUserLoginRequest request) {
        return chatPresenceService.login(request);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody ChatUserLogoutRequest request) {
        chatPresenceService.logout(request);
    }
}
