package com.angular.backend.taskmanagement;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StoryDiscussionMessageRepository extends JpaRepository<StoryDiscussionMessage, Long> {

    List<StoryDiscussionMessage> findByStoryIdOrderByCreatedAtAscIdAsc(Long storyId);

    List<StoryDiscussionMessage> findByStoryProjectIdAndAuthorIdNotOrderByCreatedAtDesc(Long projectId, String authorId);

    boolean existsByAuthorId(String authorId);
}
