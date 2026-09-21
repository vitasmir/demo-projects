package com.angular.backend.taskmanagement;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserStoryRepository extends JpaRepository<UserStory, Long> {

    List<UserStory> findByProjectIdOrderByStoryNumberAsc(Long projectId);

    List<UserStory> findBySprintIdOrderByStoryNumberAsc(Long sprintId);

    List<UserStory> findByProjectIdAndSprintIsNullOrderByStoryNumberAsc(Long projectId);

    List<UserStory> findByProjectIdAndSprintIdOrderByStoryNumberAsc(Long projectId, Long sprintId);

    @Query("select coalesce(max(s.storyNumber), 0) from UserStory s where s.project.id = :projectId")
    int findMaxStoryNumberByProjectId(@Param("projectId") Long projectId);
}
