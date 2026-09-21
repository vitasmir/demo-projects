package com.angular.backend.employees;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class EmployeeCsvExportService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeCsvExportService.class);
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter EXPORT_DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final String LEGACY_CHANGE_TITLE = "Intra-day employee changes";
    private static final String[] CHANGE_HEADER = {
            "timestamp", "action", "employee_id", "old_name", "new_name", "old_first_name", "new_first_name", "old_last_name", "new_last_name", "old_position", "new_position",
            "old_extn", "new_extn", "old_salary", "new_salary", "old_start_date", "new_start_date",
            "old_office", "new_office", "old_has_manager_rights", "new_has_manager_rights", "old_manager_id",
            "new_manager_id", "old_manager_name", "new_manager_name"
    };
    private static final String[] EMPLOYEE_HEADER = {
            "employee_id", "name", "first_name", "last_name", "position", "extn", "salary", "start_date", "office", "has_manager_rights",
            "manager_id", "manager_name"
    };

    private final EmployeeRepository employeeRepository;
    private final Path exportDirectory;
    private final String changesFileName;
    private final String completeEmployeesFileName;
    private final ZoneId zoneId;

    public EmployeeCsvExportService(
            EmployeeRepository employeeRepository,
            @Value("${app.employee-export.directory:${user.home}}") String exportDirectory,
            @Value("${app.employee-export.changes-file:intra_day_changes.csv}") String changesFileName,
            @Value("${app.employee-export.complete-file:complete_employees.csv}") String completeEmployeesFileName,
            @Value("${app.employee-export.time-zone:Europe/Prague}") String timeZone) {
        this.employeeRepository = employeeRepository;
        this.exportDirectory = Path.of(exportDirectory);
        this.changesFileName = changesFileName;
        this.completeEmployeesFileName = completeEmployeesFileName;
        this.zoneId = ZoneId.of(timeZone);
    }

    public synchronized void recordChange(String action, EmployeeJPA oldEmployee, EmployeeJPA newEmployee) {
        Path file = exportDirectory.resolve(changesFileName);
        String timestamp = LocalDateTime.now(zoneId).format(TIMESTAMP_FORMAT);
        StringBuilder row = new StringBuilder(timestamp).append(',').append(csv(action));
        appendEmployeePair(row, oldEmployee, newEmployee);
        appendChangeFileHeaderIfNeeded(file);
        appendLine(file, row.toString());
    }

    public synchronized String readIntradayChanges() throws IOException {
        Path changesFile = exportDirectory.resolve(changesFileName);
        if (!Files.exists(changesFile)) {
            return joinCsv(List.of(CHANGE_HEADER)) + '\n';
        }

        String content = normalizeChangeFile(Files.readString(changesFile, StandardCharsets.UTF_8));
        Files.delete(changesFile);
        log.info("Exported and cleared employee changes from {}", changesFile);
        return content;
    }

    public synchronized void clearIntradayChanges() throws IOException {
        Path changesFile = exportDirectory.resolve(changesFileName);
        if (Files.deleteIfExists(changesFile)) {
            log.info("Cleared pending employee changes from {}", changesFile);
        }
    }

        private List<EmployeeJPA> depthFirstEmployees(List<EmployeeJPA> employees) {
            Map<String, List<EmployeeJPA>> childrenByManager = new HashMap<>();
            List<EmployeeJPA> roots = new ArrayList<>();
            for (EmployeeJPA employee : employees) {
                String managerId = employee.getManager() == null ? null : employee.getManager().getId();
                if (managerId == null || employees.stream().noneMatch(candidate -> candidate.getId().equals(managerId))) {
                    roots.add(employee);
                } else {
                    childrenByManager.computeIfAbsent(managerId, ignored -> new ArrayList<>()).add(employee);
                }
            }
            roots.sort(Comparator.comparing(EmployeeJPA::getId));
            childrenByManager.values().forEach(children -> children.sort(Comparator.comparing(EmployeeJPA::getId)));
            List<EmployeeJPA> ordered = new ArrayList<>();
            ArrayDeque<EmployeeJPA> stack = new ArrayDeque<>();
            for (int index = roots.size() - 1; index >= 0; index--) {
                stack.push(roots.get(index));
            }
            while (!stack.isEmpty()) {
                EmployeeJPA employee = stack.pop();
                ordered.add(employee);
                List<EmployeeJPA> children = childrenByManager.getOrDefault(employee.getId(), List.of());
                for (int index = children.size() - 1; index >= 0; index--) {
                    stack.push(children.get(index));
                }
            }
            return ordered;
        }

    private String normalizeLegacyChangeLine(String line) {
        List<String> fields = parseCsvLine(line);
        if ("UPDATE".equals(fields.get(1)) && fields.size() == CHANGE_HEADER.length + 1) {
            List<String> normalized = new java.util.ArrayList<>(fields.subList(0, 3));
            for (int index = 0; index < 9; index++) {
                normalized.add(fields.get(3 + index));
                normalized.add(fields.get(13 + index));
            }
            return joinCsv(normalized);
        }
        if ("CREATE".equals(fields.get(1)) && fields.size() >= CHANGE_HEADER.length) {
            int employeeIdIndex = -1;
            for (int index = 2; index < fields.size(); index++) {
                if (!fields.get(index).isBlank()) {
                    employeeIdIndex = index;
                    break;
                }
            }
            if (employeeIdIndex > 2) {
                List<String> normalized = new java.util.ArrayList<>(fields.subList(0, 2));
                normalized.add(fields.get(employeeIdIndex));
                List<String> newFields = fields.subList(employeeIdIndex + 1, fields.size());
                for (int index = 0; index < 9; index++) {
                    normalized.add("");
                    normalized.add(index < newFields.size() ? newFields.get(index) : "");
                }
                return joinCsv(normalized);
            }
        }
        return line;
    }

    private List<String> parseCsvLine(String line) {
        List<String> fields = new java.util.ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < line.length(); index++) {
            char character = line.charAt(index);
            if (character == '"' && index + 1 < line.length() && line.charAt(index + 1) == '"' && quoted) {
                field.append('"');
                index++;
            } else if (character == '"') {
                quoted = !quoted;
            } else if (character == ',' && !quoted) {
                fields.add(field.toString());
                field.setLength(0);
            } else {
                field.append(character);
            }
        }
        fields.add(field.toString());
        return fields;
    }

    @Scheduled(cron = "${app.employee-export.cron:0 59 23 * * *}", zone = "${app.employee-export.time-zone:Europe/Prague}")
    public synchronized void exportCompleteEmployees() {
        String exportDate = LocalDateTime.now(zoneId).format(EXPORT_DATE_FORMAT);
        Path file = exportDirectory.resolve(datedFileName(completeEmployeesFileName, exportDate));
        try {
            Files.createDirectories(exportDirectory);
            exportDailyChanges(exportDate);

            StringBuilder content = new StringBuilder();
            content.append(joinCsv(List.of(EMPLOYEE_HEADER))).append('\n');
            for (EmployeeJPA employee : depthFirstEmployees(employeeRepository.findAllWithManagers())) {
                content.append(employeeRow(employee)).append('\n');
            }
            Files.writeString(file, content.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            log.info("Exported {} employees to {}", employeeRepository.count(), file);
        } catch (IOException exception) {
            log.error("Could not write complete employee export to {}", file, exception);
        }
    }

    private void exportDailyChanges(String exportDate) throws IOException {
        Path changesFile = exportDirectory.resolve(changesFileName);
        String content = Files.exists(changesFile)
                ? normalizeChangeFile(Files.readString(changesFile, StandardCharsets.UTF_8))
                : joinCsv(List.of(CHANGE_HEADER)) + '\n';
        Path dailyChangesFile = exportDirectory.resolve(datedFileName(changesFileName, exportDate));
        Files.writeString(dailyChangesFile, content, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        Files.deleteIfExists(changesFile);
        log.info("Exported daily employee changes to {}", dailyChangesFile);
    }

    private String datedFileName(String fileName, String exportDate) {
        int extensionIndex = fileName.lastIndexOf('.');
        if (extensionIndex <= 0) {
            return fileName + '_' + exportDate;
        }
        return fileName.substring(0, extensionIndex) + '_' + exportDate + fileName.substring(extensionIndex);
    }

    private void appendChangeFileHeaderIfNeeded(Path file) {
        try {
            Files.createDirectories(exportDirectory);
            if (!Files.exists(file) || Files.size(file) == 0) {
                Files.writeString(file, joinCsv(List.of(CHANGE_HEADER)) + '\n', StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } else {
                String content = Files.readString(file, StandardCharsets.UTF_8);
                String legacyPrefix = LEGACY_CHANGE_TITLE + '\n';
                if (content.startsWith(legacyPrefix)) {
                    content = content.substring(legacyPrefix.length());
                }
                Files.writeString(file, normalizeChangeFile(content), StandardCharsets.UTF_8,
                        StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not initialize employee changes file " + file, exception);
        }
    }

    private String normalizeChangeFile(String content) {
        String header = joinCsv(List.of(CHANGE_HEADER));
        StringBuilder normalized = new StringBuilder(header).append('\n');
        for (String line : content.split("\\R")) {
            if (!line.isBlank() && !line.equals(header) && !line.equals(LEGACY_CHANGE_TITLE)) {
                normalized.append(normalizeLegacyChangeLine(line)).append('\n');
            }
        }
        return normalized.toString();
    }

    private void appendLine(Path file, String line) {
        try {
            Files.writeString(file, line + '\n', StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not write employee change to " + file, exception);
        }
    }

    private void appendEmployeePair(StringBuilder row, EmployeeJPA oldEmployee, EmployeeJPA newEmployee) {
        EmployeeJPA employeeWithId = newEmployee != null ? newEmployee : oldEmployee;
        row.append(',').append(csv(employeeWithId == null ? null : employeeWithId.getId()));
        appendPairField(row, oldEmployee == null ? null : oldEmployee.getName(), newEmployee == null ? null : newEmployee.getName());
        appendPairField(row, oldEmployee == null ? null : oldEmployee.getFirstName(), newEmployee == null ? null : newEmployee.getFirstName());
        appendPairField(row, oldEmployee == null ? null : oldEmployee.getLastName(), newEmployee == null ? null : newEmployee.getLastName());
        appendPairField(row, oldEmployee == null ? null : oldEmployee.getPosition(), newEmployee == null ? null : newEmployee.getPosition());
        appendPairField(row, oldEmployee == null ? null : oldEmployee.getExtn(), newEmployee == null ? null : newEmployee.getExtn());
        appendPairField(row, oldEmployee == null ? null : oldEmployee.getSalary(), newEmployee == null ? null : newEmployee.getSalary());
        appendPairField(row, oldEmployee == null ? null : oldEmployee.getStart_date(), newEmployee == null ? null : newEmployee.getStart_date());
        appendPairField(row, oldEmployee == null ? null : oldEmployee.getOffice(), newEmployee == null ? null : newEmployee.getOffice());
        appendPairField(row, oldEmployee == null ? null : oldEmployee.isHasManagerRights(), newEmployee == null ? null : newEmployee.isHasManagerRights());
        appendPairField(row, oldEmployee == null || oldEmployee.getManager() == null ? null : oldEmployee.getManager().getId(),
                newEmployee == null || newEmployee.getManager() == null ? null : newEmployee.getManager().getId());
        appendPairField(row, oldEmployee == null || oldEmployee.getManager() == null ? null : oldEmployee.getManager().getName(),
                newEmployee == null || newEmployee.getManager() == null ? null : newEmployee.getManager().getName());
    }

    private void appendPairField(StringBuilder row, Object oldValue, Object newValue) {
        row.append(',').append(csv(oldValue)).append(',').append(csv(newValue));
    }

    private String employeeRow(EmployeeJPA employee) {
        String[] values = {
                employee.getId(), employee.getName(), employee.getFirstName(), employee.getLastName(), employee.getPosition(), employee.getExtn(), employee.getSalary(),
                String.valueOf(employee.getStart_date()), employee.getOffice(), String.valueOf(employee.isHasManagerRights()),
                employee.getManager() == null ? null : employee.getManager().getId(),
                employee.getManager() == null ? null : employee.getManager().getName()
        };
        return joinCsv(java.util.Arrays.asList(values));
    }

    private String joinCsv(List<String> values) {
        return values.stream().map(this::csv).reduce((left, right) -> left + ',' + right).orElse("");
    }

    private String csv(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value);
        return '"' + text.replace("\"", "\"\"") + '"';
    }
}