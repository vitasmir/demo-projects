package com.angular.backend.chat.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.angular.backend.chat.model.ChatGroupEntity;

public interface ChatGroupRepository extends JpaRepository<ChatGroupEntity, String> {

    boolean existsByNameIgnoreCase(String name);

    Optional<ChatGroupEntity> findByNameIgnoreCase(String name);
}
