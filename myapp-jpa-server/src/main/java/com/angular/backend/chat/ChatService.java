package com.angular.backend.chat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.angular.backend.chat.dto.ChatGroupDto;
import com.angular.backend.chat.dto.ChatMessageDto;
import com.angular.backend.chat.dto.CreateChatGroupRequest;
import com.angular.backend.chat.dto.IncomingChatMessage;
import com.angular.backend.chat.model.ChatGroupEntity;
import com.angular.backend.chat.model.ChatMessageEntity;
import com.angular.backend.chat.repo.ChatGroupRepository;
import com.angular.backend.chat.repo.ChatMessageRepository;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ChatService {

    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("^#(?:[0-9a-fA-F]{3}|[0-9a-fA-F]{6})$");

    private final ChatGroupRepository chatGroupRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatService(
            ChatGroupRepository chatGroupRepository,
            ChatMessageRepository chatMessageRepository,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.chatGroupRepository = chatGroupRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional(readOnly = true)
    public List<ChatGroupDto> listGroups() {
        return chatGroupRepository
                .findAll(Sort.by(Sort.Direction.ASC, "name"))
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public ChatGroupDto createGroup(CreateChatGroupRequest request) {
        String normalizedName = normalize(request.name());
        if (normalizedName.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Group name cannot be blank");
        }

        if (chatGroupRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new ResponseStatusException(BAD_REQUEST, "Group already exists");
        }

        ChatGroupEntity group = new ChatGroupEntity();
        group.setName(normalizedName);
        group.setCreatedAt(Instant.now());

        ChatGroupDto created = toDto(chatGroupRepository.save(group));
        messagingTemplate.convertAndSend("/topic/chat.groups", Map.of(
            "type", "CREATE",
            "group", created
        ));
        return created;
    }

    @Transactional
    public void deleteGroup(String groupId) {
        ChatGroupEntity group = chatGroupRepository
            .findById(groupId)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Group not found"));

        chatMessageRepository.deleteByGroup_Id(groupId);
        chatGroupRepository.deleteById(groupId);
        messagingTemplate.convertAndSend("/topic/chat.groups", Map.of(
            "type", "DELETE",
            "group", toDto(group)
        ));
    }

    @Transactional(readOnly = true)
    public List<ChatMessageDto> getLastMessages(String groupId) {
        if (!chatGroupRepository.existsById(groupId)) {
            throw new ResponseStatusException(NOT_FOUND, "Group not found");
        }

        List<ChatMessageEntity> newestFirst = chatMessageRepository.findTop50ByGroup_IdOrderBySentAtDesc(groupId);
        List<ChatMessageEntity> oldestFirst = new ArrayList<>(newestFirst);
        oldestFirst.sort(Comparator.comparing(ChatMessageEntity::getSentAt));
        return oldestFirst.stream().map(this::toDto).toList();
    }

    @Transactional
    public ChatMessageDto saveAndBroadcast(IncomingChatMessage payload) {
        ChatGroupEntity group = chatGroupRepository
                .findById(payload.groupId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Group not found"));

        String nickname = normalize(payload.nickname());
        String content = normalize(payload.content());
        String color = normalize(payload.color());

        if (nickname.isBlank() || content.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Nickname and content are required");
        }

        if (!HEX_COLOR_PATTERN.matcher(color).matches()) {
            throw new ResponseStatusException(BAD_REQUEST, "Color must be a valid hex color");
        }

        ChatMessageEntity entity = new ChatMessageEntity();
        entity.setGroup(group);
        entity.setNickname(nickname);
        entity.setColor(color);
        entity.setContent(content);
        entity.setSentAt(Instant.now());

        ChatMessageDto dto = toDto(chatMessageRepository.save(entity));
        messagingTemplate.convertAndSend("/topic/chat.group." + dto.groupId(), dto);
        return dto;
    }

    private ChatGroupDto toDto(ChatGroupEntity group) {
        return new ChatGroupDto(group.getId(), group.getName(), group.getCreatedAt().toString());
    }

    private ChatMessageDto toDto(ChatMessageEntity message) {
        return new ChatMessageDto(
                message.getId(),
                message.getGroup().getId(),
                message.getNickname(),
                message.getColor(),
                message.getContent(),
                message.getSentAt().toString()
        );
    }

    private String normalize(String input) {
        if (input == null) {
            return "";
        }
        return input.trim();
    }
}
