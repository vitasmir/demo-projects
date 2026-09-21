package com.angular.backend.taskmanagement;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class TestMock {
    public static void main(String[] args) {
        LocalDate day = LocalDate.now();
        List<TaskItem> storyTasks = List.of(
            createTask(TaskStatus.DONE, LocalDateTime.now()),
            createTask(TaskStatus.TODO, null)
        );

        boolean isCompleted = !storyTasks.isEmpty() && storyTasks.stream().allMatch(task -> 
                                task.getStatus() == TaskStatus.DONE && 
                                task.getCompletedAt() != null && 
                                !task.getCompletedAt().isAfter(LocalDateTime.of(day, LocalTime.MAX)));
        System.out.println("Result: " + isCompleted);
    }

    private static TaskItem createTask(TaskStatus status, LocalDateTime completedAt) {
        TaskItem t = new TaskItem();
        t.setStatus(status);
        t.setCompletedAt(completedAt);
        return t;
    }
}
