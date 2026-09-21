package com.angular.backend.chat.dto;

public record ChatUserDto(
        String nickname,
        String color,
        String joinedAt
) {
}
