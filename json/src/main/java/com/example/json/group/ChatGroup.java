package com.example.json.group;

import java.time.Instant;
import jakarta.persistence.*;

@Entity
@Table(name = "chat_group", uniqueConstraints = @UniqueConstraint(columnNames = "name"))
public class ChatGroup {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private Instant createdAt = Instant.now();

	public ChatGroup() {}

	public ChatGroup(String name) {
		this.name = name;
		this.createdAt = Instant.now();
	}

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }

	public String getName() { return name; }
	public void setName(String name) { this.name = name; }

	public Instant getCreatedAt() { return createdAt; }
	public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
