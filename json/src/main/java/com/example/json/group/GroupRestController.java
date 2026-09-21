package com.example.json.group;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/groups")
@CrossOrigin(origins = "*")
public class GroupRestController {

	private final GroupService service;
	private final SimpMessagingTemplate messagingTemplate;

	public GroupRestController(GroupService service, SimpMessagingTemplate messagingTemplate) {
		this.service = service;
		this.messagingTemplate = messagingTemplate;
	}

	@GetMapping
	public List<ChatGroup> list() {
		return service.listAll();
	}

	@PostMapping
	public ResponseEntity<?> create(@RequestBody Map<String, String> body) {
		try {
			String name = body.get("name");
			var created = service.create(name);
			// notify clients about new group
			messagingTemplate.convertAndSend("/topic/groups", new GroupEvent("CREATE", created));
			return ResponseEntity.ok(created);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
		}
	}

	@PutMapping("/{id}")
	public ResponseEntity<?> rename(@PathVariable Long id, @RequestBody Map<String, String> body) {
		try {
			var updated = service.rename(id, body.get("name"));
			// notify clients about update
			messagingTemplate.convertAndSend("/topic/groups", new GroupEvent("UPDATE", updated));
			return ResponseEntity.ok(updated);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
		}
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<?> delete(@PathVariable Long id) {
		// capture entity before deletion to inform clients
		var deleted = service.delete(id);
		if (deleted != null) {
			messagingTemplate.convertAndSend("/topic/groups", new GroupEvent("DELETE", deleted));
		}
		return ResponseEntity.noContent().build();
	}
}
