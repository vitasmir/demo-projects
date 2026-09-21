package com.angular.backend.employees;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;

@Service
@Transactional
public class EmployeeService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeService.class);

    private final EmployeeRepository employeeRepository;
    private final TreeBuilder treeBuilder;
    private final RabbitTemplate rabbitTemplate;
    private final EmployeeCsvExportService employeeCsvExportService;

    public EmployeeService(EmployeeRepository employeeRepository, TreeBuilder treeBuilder,
            RabbitTemplate rabbitTemplate, EmployeeCsvExportService employeeCsvExportService) {
        this.employeeRepository = employeeRepository;
        this.treeBuilder = treeBuilder;
        this.rabbitTemplate = rabbitTemplate;
        this.employeeCsvExportService = employeeCsvExportService;
    }

    public boolean existsEmployeeByName(String name) {
        log.info("Checking if employee exists by name: {}", name);
        boolean exists = employeeRepository.existsByName(name);
        log.info("Employee with name '{}' exists: {}", name, exists);
        return exists;
    }

    public List<EmployeeJPA> getEmployeeTree() {
        log.info("Building employee tree");
        return treeBuilder.buildTree();
    }

    public List<EmployeeJPA> getPotentialManagers() {
        log.info("Fetching potential managers");
        List<EmployeeJPA> managers = employeeRepository.findPotentialManagers();
        log.info("Found {} potential managers", managers.size());
        return managers;
    }

    public EmployeeJPA createEmployee(EmployeeJPA employee) {
        log.info("Creating employee with no specified manager");
        return createEmployee(employee, null);
    }

    public EmployeeJPA createEmployee(EmployeeJPA employee, String managerId) {
        validateRequiredFields(employee);
        log.info("Attempting to create new employee '{}' with manager ID: {}", employee.getName(), managerId);
        employee.setId(null);
        if (managerId != null && !managerId.isBlank()) {
            log.debug("Finding manager with ID: {}", managerId);
            EmployeeJPA manager = employeeRepository.findById(managerId)
                    .orElseThrow(() -> new EntityNotFoundException("Manager not found with id: " + managerId));
            employee.setManager(manager);
            log.debug("Assigned manager '{}' to new employee", manager.getName());
        }
        EmployeeJPA savedEmployee = employeeRepository.save(employee);
        log.info("Successfully created employee with ID: {}", savedEmployee.getId());
        // Fetch the employee with manager to ensure all details are present for the message
        EmployeeJPA employeeWithManager = employeeRepository.findByIdWithManager(savedEmployee.getId());
        // Send to the topic exchange with the 'new' routing key
        rabbitTemplate.convertAndSend(RabbitMQConfig.TOPIC_EXCHANGE_NAME, RabbitMQConfig.ROUTING_KEY_NEW,
                employeeWithManager);
        employeeCsvExportService.recordChange("CREATE", null, employeeWithManager);
        return employeeWithManager;
    }

    public List<EmployeeJPA> getAllEmployees() {
        log.info("Fetching all employees");
        List<EmployeeJPA> employees = employeeRepository.findAllWithManagers();
        log.info("Found {} employees", employees.size());
        return employees;
    }

    public List<EmployeeJPA> getAllEmployeesWithManagers() {
        log.info("Fetching all employees with their managers");
        List<EmployeeJPA> employees = employeeRepository.findAllWithManagers();
        log.info("Found {} employees with manager data", employees.size());
        return employees;
    }

    public String readIntradayChanges() throws java.io.IOException {
        return employeeCsvExportService.readIntradayChanges();
    }

    public void clearIntradayChanges() throws java.io.IOException {
        employeeCsvExportService.clearIntradayChanges();
    }

    public List<EmployeeJPA> restoreEmployees(List<EmployeeRestoreRequest> requests) {
        return restoreEmployees(requests, true);
    }

    public List<EmployeeJPA> restoreEmployees(List<EmployeeRestoreRequest> requests, boolean recordChanges) {
        log.debug("Starting employee import with {} records", requests == null ? null : requests.size());
        try {
            if (requests == null) {
                throw new IllegalArgumentException("Restore data must not be null.");
            }
            Map<String, EmployeeRestoreRequest> byId = new HashMap<>();
            for (EmployeeRestoreRequest request : requests) {
                log.debug("Validating imported employee record: {}", request);
                if (request == null || request.id() == null || request.id().isBlank()
                        || byId.put(request.id(), request) != null) {
                    throw new IllegalArgumentException("Restore data contains a missing or duplicate employee ID.");
                }
                validateRequiredFields(request);
            }
            for (EmployeeRestoreRequest request : requests) {
                if (Objects.equals(request.id(), request.managerId())) {
                    throw new IllegalArgumentException("An employee cannot be their own manager.");
                }
                if (request.managerId() != null && !request.managerId().isBlank()
                        && !byId.containsKey(request.managerId())
                        && !employeeRepository.existsById(request.managerId())) {
                    throw new IllegalArgumentException("Restore data references a missing manager ID: " + request.managerId());
                }
                if (byId.containsKey(request.managerId())) {
                    validateRestorePath(request, byId, new HashSet<>());
                }
            }
            log.debug("Employee import validation completed for {} records", requests.size());
            for (EmployeeRestoreRequest request : requests) {
                log.debug("Importing employee {}", request.id());
                employeeRepository.restoreEmployee(request.id(), request.name(), request.firstName(), request.lastName(), request.position(), request.extn(),
                        request.salary(), request.start_date(), request.office(), request.hasManagerRights());
            }
            for (EmployeeRestoreRequest request : requests) {
                if (request.managerId() != null && !request.managerId().isBlank()) {
                    log.debug("Assigning manager {} to imported employee {}", request.managerId(), request.id());
                    employeeRepository.restoreEmployeeManager(request.id(), request.managerId());
                }
            }
            List<EmployeeJPA> importedEmployees = employeeRepository.findAllWithManagers();
            if (recordChanges) {
                for (EmployeeRestoreRequest request : requests) {
                    EmployeeJPA importedEmployee = employeeRepository.findByIdWithManager(request.id());
                    employeeCsvExportService.recordChange("CREATE", null, importedEmployee);
                }
            }
            log.debug("Employee import completed successfully; database contains {} employees", importedEmployees.size());
            return importedEmployees;
        } catch (RuntimeException exception) {
            log.error("Employee import failed for {} records", requests == null ? null : requests.size(), exception);
            throw exception;
        }
    }

    private void validateRestorePath(EmployeeRestoreRequest request,
            Map<String, EmployeeRestoreRequest> byId, Set<String> path) {
        if (request.managerId() == null || request.managerId().isBlank()) {
            return;
        }
        if (!path.add(request.id())) {
            throw new IllegalArgumentException("Restore data contains a hierarchy cycle.");
        }
        validateRestorePath(byId.get(request.managerId()), byId, path);
        path.remove(request.id());
    }

    public EmployeeJPA getEmployeeById(String id) {
        log.info("Fetching employee by ID: {}", id);
        return employeeRepository.findById(id).orElse(null);
    }

    /**
     * Checks if setting a new manager for an employee would create a cycle in
     * the hierarchy. A cycle occurs if the employee is an ancestor of their
     * proposed new manager.
     *
     * @param employee The employee being updated.
     * @param potentialNewManager The proposed new manager.
     * @throws IllegalArgumentException if a cycle is detected.
     */
    private void detectCycle(EmployeeJPA employee, EmployeeJPA potentialNewManager) {
        log.debug("Detecting cycle for employee ID {} with potential new manager ID {}", employee.getId(), potentialNewManager != null ? potentialNewManager.getId() : "null");
        if (potentialNewManager == null) {
            return; // No new manager, no cycle is being introduced.
        }

        EmployeeJPA backupManager = employee.getManager();
        employee.setManager(potentialNewManager);

        EmployeeJPA current = potentialNewManager;
        while (current != null) {
            if (current.equals(employee)) {
                // This means the employee is an ancestor of the potential new manager,
                // so setting this relationship would create a cycle.
                employee.setManager(backupManager);
                log.warn("Cycle detected: Employee {} is an ancestor of potential manager {}", employee.getId(), potentialNewManager.getId());
                throw new IllegalArgumentException("Setting this manager would create a cycle in the employee hierarchy.");
            }
            current = current.getManager();
        }

        employee.setManager(backupManager);
    }

    private boolean isDescendantOf(EmployeeJPA possibleDescendant, EmployeeJPA possibleAncestor) {
        EmployeeJPA current = possibleDescendant;
        while (current != null) {
            if (current.equals(possibleAncestor)) {
                return true;
            }
            current = current.getManager();
        }
        return false;
    }

    public List<EmployeeJPA> moveChildrenToManager(String employeeId, String managerId) {
        log.info("Moving children of employee {} to manager {}", employeeId, managerId);
        EmployeeJPA employee = employeeRepository.findByIdWithManager(employeeId);
        if (employee == null) {
            throw new EntityNotFoundException("Employee not found with id: " + employeeId);
        }

        EmployeeJPA newManager = employeeRepository.findByIdWithManager(managerId);
        if (newManager == null) {
            throw new EntityNotFoundException("Manager not found with id: " + managerId);
        }

        if (employee.equals(newManager)) {
            throw new IllegalArgumentException("Selected employee cannot receive their own children.");
        }

        if (isDescendantOf(newManager, employee)) {
            throw new IllegalArgumentException("New manager cannot be a child of the selected employee.");
        }

        List<EmployeeJPA> children = employeeRepository.findByManagerId(employeeId);
        Map<String, EmployeeJPA> previousStates = new HashMap<>();
        for (EmployeeJPA child : children) {
            previousStates.put(child.getId(), copyEmployeeState(child));
            detectCycle(child, newManager);
            child.setManager(newManager);
        }

        List<EmployeeJPA> savedChildren = employeeRepository.saveAll(children);
        for (EmployeeJPA child : savedChildren) {
            rabbitTemplate.convertAndSend(RabbitMQConfig.TOPIC_EXCHANGE_NAME, RabbitMQConfig.ROUTING_KEY_UPDATED, child);
            employeeCsvExportService.recordChange("UPDATE", previousStates.get(child.getId()), child);
        }
        log.info("Moved {} children from employee {} to manager {}", savedChildren.size(), employeeId, managerId);
        return savedChildren;
    }

    /**
     * Updates an existing employee's details.
     *
     * @param id The ID of the employee to update.
     * @param employeeDetails An EmployeeJPA object containing the new details.
     * @param managerId The ID of the new manager. If null, the manager is not
     * changed. If blank or "null", the manager is removed.
     * @return The updated EmployeeJPA object, or null if no employee was found
     * with the given ID.
     */
    public EmployeeJPA updateEmployee(String id, EmployeeJPA employeeDetails, String managerId) {
        return updateEmployee(id, employeeDetails, managerId, true);
    }

    public EmployeeJPA updateEmployee(String id, EmployeeJPA employeeDetails, String managerId, boolean recordChanges) {
        log.info("Attempting to update employee with ID: {}", id);
        EmployeeJPA employeeToUpdate = employeeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Employee not found with id: " + id));
        validateRequiredFields(employeeDetails);
        if (!employeeDetails.isHasManagerRights() && employeeRepository.existsByManagerId(id)) {
            throw new IllegalStateException(
                "Cannot remove manager rights while the employee has subordinates.");
        }
        log.debug("Found employee to update: {}", employeeToUpdate.getName());
        EmployeeJPA previousState = copyEmployeeState(employeeToUpdate);

        // Update fields from the incoming employee object
        employeeToUpdate.setName(employeeDetails.getName());
        employeeToUpdate.setFirstName(employeeDetails.getFirstName());
        employeeToUpdate.setLastName(employeeDetails.getLastName());
        employeeToUpdate.setPosition(employeeDetails.getPosition());
        employeeToUpdate.setSalary(employeeDetails.getSalary());
        employeeToUpdate.setStart_date(employeeDetails.getStart_date());
        employeeToUpdate.setOffice(employeeDetails.getOffice());
        employeeToUpdate.setExtn(employeeDetails.getExtn());
        employeeToUpdate.setHasManagerRights(employeeDetails.isHasManagerRights());

        // Only update the manager if a managerId is provided in the request.
        // If managerId is null, the existing manager is preserved.
        if (managerId != null) {
            log.debug("Manager ID provided for update: {}", managerId);
            if (managerId.isBlank() || "null".equalsIgnoreCase(managerId)) {
                // A blank or "null" managerId indicates the manager should be removed.
                log.info("Removing manager from employee {}", id);
                employeeToUpdate.setManager(null);
            } else {
                // An employee cannot be their own manager.
                if (id.equals(managerId)) {
                    log.warn("Attempt to make employee {} their own manager", id);
                    throw new IllegalArgumentException("An employee cannot be their own manager.");
                }
                log.debug("Finding new manager with ID: {}", managerId);
                EmployeeJPA newManager = employeeRepository.findById(managerId)
                        .orElseThrow(() -> new EntityNotFoundException("Manager not found with id: " + managerId));
                detectCycle(employeeToUpdate, newManager); // Cycle detection
                employeeToUpdate.setManager(newManager);
                log.info("Set new manager for employee {} to {}", id, newManager.getId());
            }
        }

        employeeRepository.save(employeeToUpdate);
        log.debug("");
        EmployeeJPA employeeWithManager = employeeRepository.findByIdWithManager(id);
        Map<String, EmployeeJPA> updateEvent = Map.of(
                "previousState", previousState,
                "newState", employeeWithManager);
        rabbitTemplate.convertAndSend(RabbitMQConfig.TOPIC_EXCHANGE_NAME, RabbitMQConfig.ROUTING_KEY_UPDATED,
                updateEvent);
        if (recordChanges) {
            employeeCsvExportService.recordChange("UPDATE", previousState, employeeWithManager);
        }
        log.info("Successfully updated employee with ID: {}", id);
        return employeeWithManager;
    }

    private void validateRequiredFields(EmployeeJPA employee) {
        if (employee == null) {
            throw new IllegalArgumentException("Employee data must not be null.");
        }
        validateRequiredField(employee.getName(), "name");
        validateRequiredField(employee.getFirstName(), "firstName");
        validateRequiredField(employee.getLastName(), "lastName");
        validateRequiredField(employee.getPosition(), "position");
        validateRequiredField(employee.getExtn(), "extn");
        validateRequiredField(employee.getSalary(), "salary");
        if (employee.getStart_date() == null) {
            throw new IllegalArgumentException("start_date is required.");
        }
        validateRequiredField(employee.getOffice(), "office");
    }

    private void validateRequiredFields(EmployeeRestoreRequest employee) {
        if (employee == null) {
            throw new IllegalArgumentException("Employee data must not be null.");
        }
        validateRequiredField(employee.name(), "name");
        validateRequiredField(employee.firstName(), "firstName");
        validateRequiredField(employee.lastName(), "lastName");
        validateRequiredField(employee.position(), "position");
        validateRequiredField(employee.extn(), "extn");
        validateRequiredField(employee.salary(), "salary");
        if (employee.start_date() == null) {
            throw new IllegalArgumentException("start_date is required.");
        }
        validateRequiredField(employee.office(), "office");
    }

    private void validateRequiredField(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
    }

    private EmployeeJPA copyEmployeeState(EmployeeJPA employee) {
        EmployeeJPA copy = new EmployeeJPA(employee.getId(), employee.getName(), employee.getFirstName(), employee.getLastName(), employee.getPosition(),
                employee.getExtn(), employee.getSalary(), employee.getStart_date(), employee.getOffice(),
                employee.getManager(), employee.isHasManagerRights());
        copy.setChildren(List.copyOf(employee.getChildren()));
        return copy;
    }

    public EmployeeJPA findByIdWithManager(String id) {
        log.info("Fetching employee by ID with manager: {}", id);
        EmployeeJPA employee = employeeRepository.findByIdWithManager(id);
        if (employee == null) {
            log.warn("Employee with ID {} not found.", id);
        }

        return employee;
    }

    public boolean deleteEmployee(String id) {
        return deleteEmployee(id, true);
    }

    public boolean deleteEmployee(String id, boolean recordChanges) {
        log.debug("Starting employee deletion for ID {}", id);
        if (!employeeRepository.existsById(id)) {
            log.debug("Employee {} was not found during deletion", id);
            return false;
        }

        if (employeeRepository.existsByManagerId(id)) {
            log.warn("Attempted to delete employee {} who is a manager.", id);
            throw new IllegalStateException("Cannot delete employee who is a manager with subordinates.");
        }

        log.debug("Loading employee {} before deletion", id);
        EmployeeJPA deletedEmployee = copyEmployeeState(employeeRepository.findByIdWithManager(id));
        log.debug("Deleting employee {} from the database", id);
        employeeRepository.deleteById(id);
        Map<String, Object> deleteEvent = new HashMap<>();
        deleteEvent.put("previousState", deletedEmployee);
        deleteEvent.put("newState", null);
        rabbitTemplate.convertAndSend(RabbitMQConfig.TOPIC_EXCHANGE_NAME, RabbitMQConfig.ROUTING_KEY_DELETED,
                deleteEvent);
        log.debug("Recording DELETE change for employee {}", id);
        if (recordChanges) {
            employeeCsvExportService.recordChange("DELETE", deletedEmployee, null);
        }
        log.info("Successfully deleted employee with ID: {}", id);
        return true;
    }

    public void deleteAllEmployees() {
        employeeRepository.truncateEmployees();
        log.info("Successfully deleted all employees");
    }

}
