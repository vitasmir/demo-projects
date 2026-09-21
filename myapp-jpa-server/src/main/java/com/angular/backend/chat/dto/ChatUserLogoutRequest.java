package com.angular.backend.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatUserLogoutRequest(
        @NotBlank(message = "Group is required")
        String groupId,

        @NotBlank(message = "Nickname is required")
        @Size(max = 32, message = "Nickname is too long")
        String nickname
) {
}
