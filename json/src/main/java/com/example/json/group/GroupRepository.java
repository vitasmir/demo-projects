package com.example.json.group;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupRepository extends JpaRepository<ChatGroup, Long> {
	Optional<ChatGroup> findByName(String name);
	boolean existsByName(String name);
}
