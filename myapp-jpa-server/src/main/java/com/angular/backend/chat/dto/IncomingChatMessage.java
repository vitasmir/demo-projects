package com.angular.backend.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record IncomingChatMessage(
        @NotBlank(message = "groupId is required")
        String groupId,
        @NotBlank(message = "nickname is required")
        @Size(max = 64, message = "nickname too long")
        String nickname,
        @NotBlank(message = "color is required")
        @Size(max = 16, message = "color too long")
        String color,
        @NotBlank(message = "content is required")
        @Size(max = 4000, message = "content too long")
        String content
) {
}
