package com.angular.backend.taskmanagement;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SprintRepository extends JpaRepository<Sprint, Long> {

	List<Sprint> findByProjectIdOrderByStartDateAscIdAsc(Long projectId);
}
