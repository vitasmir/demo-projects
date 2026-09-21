import java.util.*;

public class TestAllMatch {
    public static void main(String[] args) {
        List<String> statuses = Arrays.asList("DONE", "TODO");
        boolean allMatch = statuses.stream().allMatch(s -> "DONE".equals(s));
        System.out.println("2 tasks, 1 DONE, 1 TODO: " + allMatch);
        
        statuses = Arrays.asList("DONE", "DONE");
        allMatch = statuses.stream().allMatch(s -> "DONE".equals(s));
        System.out.println("2 tasks, 2 DONE: " + allMatch);
        
        statuses = Arrays.asList();
        allMatch = statuses.stream().allMatch(s -> "DONE".equals(s));
        System.out.println("0 tasks: " + allMatch);
    }
}
