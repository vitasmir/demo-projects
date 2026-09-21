package com.example.json.chat;

public class ChatMessage {

	public enum MessageType { CHAT, JOIN, LEAVE }

	private MessageType type;
	private String sender;
	private String content;
	private String groupId;
	private String timestamp;
	private String color;

	public ChatMessage() {}

	public MessageType getType() { return type; }
	public void setType(MessageType type) { this.type = type; }

	public String getSender() { return sender; }
	public void setSender(String sender) { this.sender = sender; }

	public String getContent() { return content; }
	public void setContent(String content) { this.content = content; }

	public String getGroupId() { return groupId; }
	public void setGroupId(String groupId) { this.groupId = groupId; }

	public String getTimestamp() { return timestamp; }
	public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

	public String getColor() { return color; }
	public void setColor(String color) { this.color = color; }
}