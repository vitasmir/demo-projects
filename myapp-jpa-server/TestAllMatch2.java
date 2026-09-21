import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

public class TestAllMatch2 {
    static class Task {
        String status;
        LocalDateTime completedAt;
        public Task(String status, LocalDateTime completedAt) {
            this.status = status;
            this.completedAt = completedAt;
        }
        public String getStatus() { return status; }
        public LocalDateTime getCompletedAt() { return completedAt; }
    }
    public static void main(String[] args) {
        LocalDateTime day = LocalDateTime.now();
        List<Task> storyTasks = Arrays.asList(
            new Task("DONE", LocalDateTime.now()),
            new Task("TODO", null)
        );
        boolean isCompleted = !storyTasks.isEmpty() && storyTasks.stream().allMatch(task -> 
                                "DONE".equals(task.getStatus()) && 
                                task.getCompletedAt() != null && 
                                !task.getCompletedAt().isAfter(day));
        System.out.println("Result: " + isCompleted);
    }
}
