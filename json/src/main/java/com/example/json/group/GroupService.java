package com.example.json.group;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class GroupService {

	private final GroupRepository repo;

	public GroupService(GroupRepository repo) {
		this.repo = repo;
	}

	public List<ChatGroup> listAll() {
		return repo.findAll();
	}

	public ChatGroup create(String name) {
		if (name == null || name.trim().isEmpty()) {
			throw new IllegalArgumentException("Group name is required");
		}
		if (repo.existsByName(name.trim())) {
			throw new IllegalArgumentException("Group already exists");
		}
		return repo.save(new ChatGroup(name.trim()));
	}

	public ChatGroup rename(Long id, String newName) {
		var g = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Group not found"));
		if (repo.existsByName(newName) && !g.getName().equals(newName)) {
			throw new IllegalArgumentException("Group name already used");
		}
		g.setName(newName);
		return repo.save(g);
	}

	public ChatGroup delete(Long id) {
		var g = repo.findById(id).orElse(null);
		if (g != null) {
			repo.deleteById(id);
		}
		return g;
	}

	/** Ensure a group exists by name, create if missing. */
	public ChatGroup ensureExistsByName(String name) {
		return repo.findByName(name).orElseGet(() -> repo.save(new ChatGroup(name)));
	}
}
