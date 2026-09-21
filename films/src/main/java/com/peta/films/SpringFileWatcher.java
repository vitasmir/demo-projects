package com.peta.films;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.*;

import static java.nio.file.StandardWatchEventKinds.*;

// 1. The Main Spring Boot Application Class
@SpringBootApplication
@EnableAsync
@Configuration
@Component
public class SpringFileWatcher {

    public static void main(String[] args) {
        SpringApplication.run(SpringFileWatcher.class, args);
    }

    // Config for async execution of the watcher loop
    @Bean
    public TaskExecutor taskExecutor() {
        return new SimpleAsyncTaskExecutor();
    }
}



@Service
class FileMonitorService implements ApplicationRunner {

    private final FilmRepository repository;
    private final WatchService watcher;
    private final Map<WatchKey, Path> keys;

    @Value("${supported.extensions}")
    private String supportedExtensions = ".mkv";

    

    @Autowired
    public FileMonitorService(FilmRepository repository) throws IOException {
        this.repository = repository;
        this.watcher = FileSystems.getDefault().newWatchService();
        this.keys = new HashMap<>();
    }

    /**
     * This method runs automatically after Spring Boot starts up.
     * It reads the CLI args and kicks off the monitoring.
     */
    @Override
    public void run(ApplicationArguments args) {
        Set<String> argsOptionNames = args.getOptionNames();
        
        args.getOptionValues("-p");

        List<String> rawPaths = args.getNonOptionArgs();
        List<Path> rootPaths = new ArrayList<>();

        if (rawPaths.isEmpty()) {
            System.out.println("No directories specified to monitor. Please provide paths as command-line arguments.");
            
            return;
        } else {
            System.out.println("Scanning the following directories:");
            for (String p : rawPaths) {
                System.out.println(p);
                rootPaths.add(Paths.get(p));
            }
            System.out.println("");
        }

        // Run the blocking loop in a separate thread so we don't freeze Spring
        startMonitoring(rootPaths);
    }

    @Async
    public void startMonitoring(List<Path> roots) {
        System.out.println("--- STARTING INITIAL SCAN ---");

        for (Path root : roots) {
            if (!Files.exists(root)) {
                try {
                    Files.createDirectories(root);
                } catch (IOException e) {
                    e.printStackTrace();
                    continue;
                }
            }
            try {
                registerAllAndScan(root);
            } catch (IOException e) {
                System.err.println("Error scanning " + root + ": " + e.getMessage());
            }
        }

        System.out.println("--- LIVE MONITORING ACTIVE ---");
        processEvents();
    }

    // --- Directory Walking & Event Logic (Same as before, adapted for Spring Repo)
    // ---

    private void registerAllAndScan(final Path start) throws IOException {
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                register(dir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (isSupportedFileExtension(file)) {
                    saveToElastic(file, attrs.size(), "CREATED (EXISTING)");
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void register(Path dir) throws IOException {
        WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        keys.put(key, dir);
    }

    private void processEvents() {
        for (;;) {
            WatchKey key;
            try {
                key = watcher.take();
            } catch (InterruptedException x) {
                return;
            }

            Path dir = keys.get(key);
            if (dir == null)
                continue;

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();

                if (kind == OVERFLOW) {
                    System.err.println("WatchService OVERFLOW for directory: " + dir + ". Performing rescan.");
                    try {
                        registerAllAndScan(dir);
                    } catch (IOException e) {
                        System.err.println("Rescan failed for " + dir + ": " + e.getMessage());
                    }
                    continue;
                }

                Path fileName = ((WatchEvent<Path>) event).context();
                Path child = dir.resolve(fileName);

                if (kind == ENTRY_CREATE) {
                    try {
                        if (Files.isDirectory(child, LinkOption.NOFOLLOW_LINKS)) {
                            registerAllAndScan(child);
                        }
                    } catch (IOException x) {
                        x.printStackTrace();
                    }
                }

                if (isSupportedFileExtension(child)) {
                    if (kind == ENTRY_CREATE) {
                        try {
                            long size = Files.size(child);
                            System.out.println("Created " + child);
                            saveToElastic(child, size, "CREATED");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else if (kind == ENTRY_DELETE) {
                        System.out.println("Deleted " + child);
                        removeFromElastic(child);
                    }
                }
                boolean valid = key.reset();
                if (!valid) {
                    keys.remove(key);
                    if (keys.isEmpty())
                        break;
                }
            }

        }
    }

    private void saveToElastic(Path path, long size, String eventType) {
        try {
            String fileName = path.getFileName().toString();
            if (!isSupportedFileExtension(path)) {
                System.out.println("File " + path + " is not supported!");
                return;
            }  
            
            String fullPath = path.toAbsolutePath().toString();
            String id = generateId(fullPath);

            Film doc = Film.builder()
                    .id(id)
                    .path(fullPath)
                    .size(size)
                    .eventType(eventType)
                    .name(fileName)
                    .timestamp(Instant.now())
                    .build();

            // Spring Data Elasticsearch handles the HTTP communication here
            repository.save(doc);

            System.out.println("Indexed [" + eventType + "]: " + fullPath);
        } catch (Exception e) {
            System.err.println("Failed to save to ES: " + e.getMessage());
        }
    }

    private boolean isSupportedFileExtension(Path path) {
        String[] exts = supportedExtensions.split(",");
        for (String ext : exts) {
            if (path.toString().toLowerCase().endsWith(ext.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private String generateId(String fullPath) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(fullPath.getBytes(StandardCharsets.UTF_8));
    }

    private void removeFromElastic(Path child) {
        try {
            String fullPath = child.toAbsolutePath().toString();
            String id = generateId(fullPath);
            repository.deleteById(id);
            System.out.println("Deleted from ES: " + fullPath);
        } catch (Exception e) {
            System.err.println("Failed to delete from ES: " + e.getMessage());
        }
    }

    
}
