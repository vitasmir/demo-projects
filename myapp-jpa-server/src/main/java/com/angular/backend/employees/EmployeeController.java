package com.angular.backend.employees;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/employees")
@Tag(name = "Employee Management", description = "APIs for managing employees and their hierarchy")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    @Operation(summary = "Create a new employee",
            description = "Creates a new employee and optionally assigns a manager.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Employee object to be created.",
                    required = true,
                    content = @Content(schema = @Schema(implementation = EmployeeJPA.class))
            ))
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Employee created successfully",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = EmployeeJPA.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input provided", content = @Content),
        @ApiResponse(responseCode = "404", description = "Manager not found", content = @Content)
    })
    @PostMapping
    public ResponseEntity<EmployeeJPA> createEmployee(
            @RequestBody EmployeeJPA employee,
            @Parameter(description = "ID of the manager for the new employee. Optional.") @RequestParam(required = false) String managerId) {
        EmployeeJPA newEmployee = employeeService.createEmployee(employee, managerId);
        return new ResponseEntity<>(newEmployee, HttpStatus.CREATED);
    }

    @Operation(summary = "Restore employees with their original IDs")
    @PostMapping("/restore")
    public ResponseEntity<List<EmployeeJPA>> restoreEmployees(
            @RequestBody List<EmployeeRestoreRequest> employees,
            @RequestParam(defaultValue = "false") boolean recordChanges) {
        return ResponseEntity.ok(employeeService.restoreEmployees(employees, recordChanges));
    }

    @Operation(summary = "Get all employees", description = "Returns a list of all employees. Manager information is not guaranteed to be present.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved list",
                content = @Content(mediaType = "application/json",
                        array = @ArraySchema(schema = @Schema(implementation = EmployeeJPA.class))))
    })
    @GetMapping
    public ResponseEntity<Iterable<EmployeeJPA>> getAllEmployees() {
        List<EmployeeJPA> allEmployees = employeeService.getAllEmployees();
        return new ResponseEntity<>(allEmployees, HttpStatus.OK);
    }

    @Operation(summary = "Export intraday employee changes")
    @GetMapping(value = "/changes", produces = "text/csv")
    public ResponseEntity<String> exportIntradayChanges() {
        try {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=intra_day_changes.csv")
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .body(employeeService.readIntradayChanges());
        } catch (java.io.IOException exception) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/changes")
    public ResponseEntity<Void> clearIntradayChanges() {
        try {
            employeeService.clearIntradayChanges();
            return ResponseEntity.noContent().build();
        } catch (java.io.IOException exception) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "Get an employee by ID, including manager details", description = "Fetches a single employee by their ID, with their manager object included.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Found the employee",
                content = {
                    @Content(mediaType = "application/json",
                            schema = @Schema(implementation = EmployeeJPA.class))}),
        @ApiResponse(responseCode = "404", description = "Employee not found",
                content = @Content)
    })
    @GetMapping("/with-manager/{id}")
    public ResponseEntity<EmployeeJPA> getEmployeeWithManager(
            @Parameter(description = "ID of the employee to be fetched") @PathVariable String id) {
        EmployeeJPA employee = employeeService.findByIdWithManager(id);
        return employee != null ? new ResponseEntity<>(employee, HttpStatus.OK) : new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @Operation(summary = "Get all employees with manager details", description = "Returns a list of all employees, with their manager objects included.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved list",
                content = @Content(mediaType = "application/json",
                        array = @ArraySchema(schema = @Schema(implementation = EmployeeJPA.class))))
    })
    @GetMapping("/with-managers")
    public ResponseEntity<Iterable<EmployeeJPA>> getAllEmployeesWithManagers() {
        List<EmployeeJPA> allEmployeesWithManagers = employeeService.getAllEmployeesWithManagers();
        return new ResponseEntity<>(allEmployeesWithManagers, HttpStatus.OK);
    }

    @Operation(summary = "Get the employee hierarchy as a tree", description = "Returns a tree structure of employees, starting from the root nodes (employees without a manager).")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved employee tree",
                content = @Content(mediaType = "application/json",
                        array = @ArraySchema(schema = @Schema(implementation = EmployeeJPA.class))))
    })
    @GetMapping("/tree")
    public ResponseEntity<Iterable<EmployeeJPA>> getEmployeeTree() {
        List<EmployeeJPA> rootEmployees = employeeService.getEmployeeTree();
        return new ResponseEntity<>(rootEmployees, HttpStatus.OK);
    }

    @Operation(summary = "Get all potential managers", description = "Returns a list of employees who can be assigned as a manager.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved list of potential managers",
                content = @Content(mediaType = "application/json",
                        array = @ArraySchema(schema = @Schema(implementation = EmployeeJPA.class))))
    })
    @GetMapping("/managers")
    public ResponseEntity<List<EmployeeJPA>> getAllManagers() {
        List<EmployeeJPA> managers = employeeService.getPotentialManagers();
        return new ResponseEntity<>(managers, HttpStatus.OK);
    }

    @Operation(summary = "Get an employee by ID", description = "Fetches a single employee by their ID. Manager details may not be included.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Found the employee",
                content = {
                    @Content(mediaType = "application/json",
                            schema = @Schema(implementation = EmployeeJPA.class))}),
        @ApiResponse(responseCode = "404", description = "Employee not found",
                content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<EmployeeJPA> getEmployeeById(
            @Parameter(description = "ID of the employee to be fetched") @PathVariable String id) {
        EmployeeJPA employee = employeeService.getEmployeeById(id);
        if (employee == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(employee, HttpStatus.OK);
    }

    @Operation(summary = "Check if an employee name exists", description = "Checks for the existence of an employee with the given name.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Check complete",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = Boolean.class)))
    })
    @GetMapping("/employeeByNameExists/{employeeName}")
    public ResponseEntity<Optional<Boolean>> isEmployeeNameExisting(
            @Parameter(description = "Name of the employee to check") @PathVariable String employeeName) {
        boolean exists = employeeService.existsEmployeeByName(employeeName);
        return new ResponseEntity<>(Optional.of(exists), HttpStatus.OK);
    }

    @Operation(summary = "Update an existing employee",
            description = "Updates an existing employee's details and/or their manager. "
            + "To remove a manager, provide 'null' or a blank string for managerId.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Employee object with updated details. The ID in the body is ignored.",
                    required = true,
                    content = @Content(schema = @Schema(implementation = EmployeeJPA.class))
            ))
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Employee updated successfully",
                content = {
                    @Content(mediaType = "application/json",
                            schema = @Schema(implementation = EmployeeJPA.class))}),
        @ApiResponse(responseCode = "400", description = "Invalid data supplied (e.g., creating a hierarchy cycle)",
                content = @Content),
        @ApiResponse(responseCode = "404", description = "Employee or Manager not found",
                content = @Content)
    })
    @PutMapping("/{id}")
    public ResponseEntity<EmployeeJPA> updateEmployee(
            @Parameter(description = "ID of the employee to update") @PathVariable String id,
            @RequestBody EmployeeJPA employee,
            @Parameter(description = "ID of the new manager. Optional. Send 'null' or blank to remove manager.") @RequestParam(required = false) String managerId,
            @RequestParam(defaultValue = "true") boolean recordChanges) {
        EmployeeJPA updatedEmployee = employeeService.updateEmployee(id, employee, managerId, recordChanges);
        if (updatedEmployee == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(updatedEmployee, HttpStatus.OK);
    }

        @Operation(summary = "Move an employee's children to another manager",
            description = "Moves all direct children of the selected employee to a new manager. The new manager cannot be the selected employee or one of their descendants.")
        @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Children moved successfully",
            content = @Content(mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = EmployeeJPA.class)))),
        @ApiResponse(responseCode = "400", description = "Invalid hierarchy move",
            content = @Content),
        @ApiResponse(responseCode = "404", description = "Employee or Manager not found",
            content = @Content)
        })
        @PutMapping("/{id}/children/manager")
        public ResponseEntity<List<EmployeeJPA>> moveChildrenToManager(
            @Parameter(description = "ID of the employee whose children should be moved") @PathVariable String id,
            @Parameter(description = "ID of the manager who should receive the children") @RequestParam String managerId) {
        List<EmployeeJPA> movedChildren = employeeService.moveChildrenToManager(id, managerId);
        return new ResponseEntity<>(movedChildren, HttpStatus.OK);
        }

    @Operation(summary = "Delete an employee", description = "Deletes an employee by their ID. Fails if the employee is a manager to other employees.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Employee deleted successfully",
                content = @Content),
        @ApiResponse(responseCode = "404", description = "Employee not found",
                content = @Content),
        @ApiResponse(responseCode = "400", description = "Cannot delete an employee who is a manager",
                content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmployee(
            @Parameter(description = "ID of the employee to delete") @PathVariable String id,
            @RequestParam(defaultValue = "true") boolean recordChanges) {
        boolean deleted = employeeService.deleteEmployee(id, recordChanges);
        if (!deleted) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @DeleteMapping("/all")
    public ResponseEntity<Void> deleteAllEmployees() {
        employeeService.deleteAllEmployees();
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}

