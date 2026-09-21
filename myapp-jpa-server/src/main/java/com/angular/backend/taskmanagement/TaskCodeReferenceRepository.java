package com.angular.backend.taskmanagement;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskCodeReferenceRepository extends JpaRepository<TaskCodeReference, Long> {
    List<TaskCodeReference> findByTaskIdOrderByCreatedAtDesc(Long taskId);
}
