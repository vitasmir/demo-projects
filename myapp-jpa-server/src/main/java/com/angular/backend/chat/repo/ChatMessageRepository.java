package com.angular.backend.chat.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.angular.backend.chat.model.ChatMessageEntity;

public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, String> {

    List<ChatMessageEntity> findTop50ByGroup_IdOrderBySentAtDesc(String groupId);

    void deleteByGroup_Id(String groupId);
}
