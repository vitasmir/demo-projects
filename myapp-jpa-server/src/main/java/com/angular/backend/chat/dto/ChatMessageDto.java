package com.angular.backend.chat.dto;

public record ChatMessageDto(
        String id,
        String groupId,
        String nickname,
        String color,
        String content,
        String sentAt
) {
}
