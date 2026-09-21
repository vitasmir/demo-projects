package com.example.json.chat;

import java.time.Instant;

import com.example.json.group.GroupService;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class ChatController {

	private final SimpMessagingTemplate messagingTemplate;
	private final GroupService groupService;

	public ChatController(SimpMessagingTemplate messagingTemplate, GroupService groupService) {
		this.messagingTemplate = messagingTemplate;
		this.groupService = groupService;
	}

	@MessageMapping("/chat/{groupId}")
	public void send(@DestinationVariable String groupId, ChatMessage message) {
		// ensure group present (creates if missing)
		groupService.ensureExistsByName(groupId);

		message.setGroupId(groupId);
		message.setTimestamp(Instant.now().toString());

		// ensure color is present and in a simple normalized form (default black)
		if (message.getColor() == null || message.getColor().trim().isEmpty()) {
			message.setColor("#000000");
		} else {
			String c = message.getColor().trim();
			// basic normalization: ensure leading '#'
			if (!c.startsWith("#")) c = "#" + c;
			message.setColor(c);
		}

		messagingTemplate.convertAndSend("/topic/group/" + groupId, message);
	}
}
