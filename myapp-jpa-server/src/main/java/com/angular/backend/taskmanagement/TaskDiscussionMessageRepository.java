package com.angular.backend.taskmanagement;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskDiscussionMessageRepository extends JpaRepository<TaskDiscussionMessage, Long> {

    List<TaskDiscussionMessage> findByTaskIdOrderByCreatedAtAscIdAsc(Long taskId);

    List<TaskDiscussionMessage> findByTaskStoryProjectIdAndAuthorIdNotOrderByCreatedAtDesc(Long projectId, String authorId);

    boolean existsByAuthorId(String authorId);
}
