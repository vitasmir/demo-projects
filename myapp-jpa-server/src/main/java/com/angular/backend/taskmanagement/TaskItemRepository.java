package com.angular.backend.taskmanagement;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskItemRepository extends JpaRepository<TaskItem, Long> {

    List<TaskItem> findByStoryIdOrderByTaskNumberAsc(Long storyId);

    @Query("select t from TaskItem t where t.story.project.id = :projectId and t.story.sprint is null order by t.story.storyNumber asc, t.taskNumber asc, t.id asc")
    List<TaskItem> findByBacklogStoryProjectIdOrderByTaskNumberAsc(@Param("projectId") Long projectId);

    @Query("select t from TaskItem t where t.story.sprint.id = :sprintId order by t.story.storyNumber asc, t.taskNumber asc, t.id asc")
    List<TaskItem> findByStorySprintIdOrderByTaskNumberAsc(Long sprintId);

    long countByStorySprintId(Long sprintId);

    long countByStorySprintIdAndCompletedAtLessThanEqual(Long sprintId, LocalDateTime dateTime);

    boolean existsByStoryId(Long storyId);

    @Query("select count(t) > 0 from TaskItem t where t.creator.id = :userId or t.assignee.id = :userId or t.reviewer.id = :userId")
    boolean existsByAnyUserAssignment(@Param("userId") String userId);

    @Query("select coalesce(max(t.taskNumber), 0) from TaskItem t where t.story.id = :storyId")
    int findMaxTaskNumberByStoryId(@Param("storyId") Long storyId);
}
