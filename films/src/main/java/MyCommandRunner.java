import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.stereotype.Component;

import com.peta.films.SpringFileWatcher;

import java.util.List;

@Component
public class MyCommandRunner implements ApplicationRunner {
    public static void main(String[] args) {
        SpringApplication.run(MyCommandRunner.class, args);
    }
    @Override
    public void run(ApplicationArguments args) {
        // 1. Check for a flag (e.g., --verbose)
        if (args.containsOption("verbose")) {
            System.out.println("Verbose mode is ACTIVE");
        }

        // 2. Get a value (e.g., --mode=production)
        if (args.containsOption("mode")) {
            // Always returns a List, because you can do --mode=a --mode=b
            List<String> modes = args.getOptionValues("mode");
            System.out.println("Current mode: " + modes.get(0));
        }

        // 3. specific handling for multi-value options (e.g., --input=a.txt --input=b.txt)
        if (args.containsOption("input")) {
            List<String> inputs = args.getOptionValues("input");
            System.out.println("Processing " + inputs.size() + " files.");
            inputs.forEach(file -> System.out.println(" - " + file));
        }
        
        // 4. Debugging: Print all options passed
        System.out.println("All Option Names: " + args.getOptionNames());
    }
}