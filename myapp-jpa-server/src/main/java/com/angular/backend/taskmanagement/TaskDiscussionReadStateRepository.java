package com.angular.backend.taskmanagement;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskDiscussionReadStateRepository extends JpaRepository<TaskDiscussionReadState, Long> {

    Optional<TaskDiscussionReadState> findByTaskIdAndUserId(Long taskId, String userId);

    List<TaskDiscussionReadState> findByUserIdAndTaskStoryProjectId(String userId, Long projectId);

    void deleteByUserId(String userId);
}
