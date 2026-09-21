package com.example.json.group;

public class GroupEvent {
	private String type; // CREATE | UPDATE | DELETE
	private ChatGroup group;

	public GroupEvent() {}

	public GroupEvent(String type, ChatGroup group) {
		this.type = type;
		this.group = group;
	}

	public String getType() { return type; }
	public void setType(String type) { this.type = type; }

	public ChatGroup getGroup() { return group; }
	public void setGroup(ChatGroup group) { this.group = group; }
}
