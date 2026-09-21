package com.angular.backend.chat;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.angular.backend.chat.dto.ChatUserDto;
import com.angular.backend.chat.dto.ChatUserLoginRequest;
import com.angular.backend.chat.dto.ChatUserLogoutRequest;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
public class ChatPresenceService {

    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("^#(?:[0-9a-fA-F]{3}|[0-9a-fA-F]{6})$");

    private final ConcurrentMap<String, ConcurrentMap<String, ChatUserDto>> onlineUsersByGroup = new ConcurrentHashMap<>();
    private final SimpMessagingTemplate messagingTemplate;

    public ChatPresenceService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public List<ChatUserDto> listOnlineUsers(String groupId) {
        String normalizedGroupId = normalize(groupId);
        if (normalizedGroupId.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Group is required");
        }

        ConcurrentMap<String, ChatUserDto> groupUsers = onlineUsersByGroup.get(normalizedGroupId);
        if (groupUsers == null) {
            return List.of();
        }

        return groupUsers
                .values()
                .stream()
                .sorted(Comparator
                        .comparing(ChatUserDto::joinedAt)
                        .thenComparing(ChatUserDto::nickname, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public ChatUserDto login(ChatUserLoginRequest request) {
        String groupId = normalize(request.groupId());
        String nickname = normalize(request.nickname());
        String color = normalize(request.color());

        if (groupId.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Group is required");
        }

        if (nickname.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Nickname is required");
        }

        if (!HEX_COLOR_PATTERN.matcher(color).matches()) {
            throw new ResponseStatusException(BAD_REQUEST, "Color must be a valid hex color");
        }

        ChatUserDto user = new ChatUserDto(nickname, color, Instant.now().toString());

        String nicknameKey = nickname.toLowerCase(Locale.ROOT);
        Set<String> affectedGroups = new HashSet<>();

        onlineUsersByGroup.forEach((existingGroupId, usersInGroup) -> {
            ChatUserDto removed = usersInGroup.remove(nicknameKey);
            if (removed != null) {
                affectedGroups.add(existingGroupId);
            }
            if (usersInGroup.isEmpty()) {
                onlineUsersByGroup.remove(existingGroupId, usersInGroup);
            }
        });

        onlineUsersByGroup
                .computeIfAbsent(groupId, _unused -> new ConcurrentHashMap<>())
                .put(nicknameKey, user);
        affectedGroups.add(groupId);

        affectedGroups.forEach(this::broadcastOnlineUsers);
        return user;
    }

    public void logout(ChatUserLogoutRequest request) {
        String groupId = normalize(request.groupId());
        String nickname = normalize(request.nickname());
        if (groupId.isBlank() || nickname.isBlank()) {
            return;
        }

        ConcurrentMap<String, ChatUserDto> usersInGroup = onlineUsersByGroup.get(groupId);
        if (usersInGroup == null) {
            return;
        }

        ChatUserDto removed = usersInGroup.remove(nickname.toLowerCase(Locale.ROOT));
        if (removed != null) {
            if (usersInGroup.isEmpty()) {
                onlineUsersByGroup.remove(groupId, usersInGroup);
            }
            broadcastOnlineUsers(groupId);
        }
    }

    private void broadcastOnlineUsers(String groupId) {
        messagingTemplate.convertAndSend("/topic/chat.users." + groupId, listOnlineUsers(groupId));
    }

    private String normalize(String input) {
        if (input == null) {
            return "";
        }
        return input.trim();
    }
}
