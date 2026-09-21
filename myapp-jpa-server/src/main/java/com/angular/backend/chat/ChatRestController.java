package com.angular.backend.chat;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.angular.backend.chat.dto.ChatGroupDto;
import com.angular.backend.chat.dto.ChatMessageDto;
import com.angular.backend.chat.dto.CreateChatGroupRequest;

import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/chat/groups")
public class ChatRestController {

    private final ChatService chatService;

    public ChatRestController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping
    public List<ChatGroupDto> listGroups() {
        return chatService.listGroups();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ChatGroupDto createGroup(@Valid @RequestBody CreateChatGroupRequest request) {
        return chatService.createGroup(request);
    }

    @DeleteMapping("/{groupId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGroup(@PathVariable String groupId) {
        chatService.deleteGroup(groupId);
    }

    @GetMapping("/{groupId}/messages")
    public List<ChatMessageDto> getLastMessages(@PathVariable String groupId) {
        return chatService.getLastMessages(groupId);
    }
}
