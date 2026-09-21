package com.angular.backend.taskmanagement;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StoryDiscussionReadStateRepository extends JpaRepository<StoryDiscussionReadState, Long> {

    Optional<StoryDiscussionReadState> findByStoryIdAndUserId(Long storyId, String userId);

    List<StoryDiscussionReadState> findByUserIdAndStoryProjectId(String userId, Long projectId);

    void deleteByUserId(String userId);
}
