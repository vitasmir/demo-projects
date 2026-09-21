package com.angular.backend.employees;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.angular.backend.AbstractIntegrationTest;

import jakarta.persistence.EntityNotFoundException;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({EmployeeService.class, TreeBuilder.class, JacksonAutoConfiguration.class})
public class EmployeeServiceTest extends AbstractIntegrationTest {

    private static final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @BeforeEach
    public void setUp() {
        // Clear the database before each test
        employeeRepository.deleteAll();
    }

    @Test
    void getAllEmployees_shouldReturnNonEmptyList() {
        // Given
        EmployeeJPA employee1 = new EmployeeJPA();
        employee1.setName("John Doe");
        employee1.setPosition("Software Engineer");
        employee1.setOffice("IT");
        employee1.setExtn("E123");
        employee1.setSalary("100000");
        employee1.setStart_date(LocalDate.parse("2023-03-03", fmt));
        employeeRepository.save(employee1);

        EmployeeJPA employee2 = new EmployeeJPA();
        employee2.setName("Jaqne Smith");
        employee2.setPosition("Data Scientist");
        employee2.setOffice("Science");
        employee2.setExtn("E456");
        employee2.setSalary("120000");
        employee2.setStart_date(LocalDate.parse("2023-03-03", fmt));
        employeeRepository.save(employee2);

        // When
        List<EmployeeJPA> employees = employeeService.getAllEmployees();

        // Then
        assertNotNull(employees);
        assertEquals(2, employees.size());
    }

    @Test
    void createEmployee_shouldReturnSavedEmployee() {
        // Given
        EmployeeJPA employee = new EmployeeJPA();
        employee.setName("New Employee");
        employee.setFirstName("New");
        employee.setLastName("Employee");
        employee.setPosition("Tester");
        employee.setOffice("IT");
        employee.setExtn("E789");
        employee.setSalary("90000");
        employee.setStart_date(LocalDate.parse("2023-03-03", fmt));

        // When
        // The createEmployee method now requires a managerId, which can be null.
        EmployeeJPA createdEmployee = employeeService.createEmployee(employee, null);

        // Then
        assertNotNull(createdEmployee);
        assertNotNull(createdEmployee.getId());
        assertEquals("New Employee", createdEmployee.getName());
        assertTrue(employeeRepository.findById(createdEmployee.getId()).isPresent());
    }

    @Test
    void getEmployeeById_whenEmployeeExists_shouldReturnEmployee() {
        // Given
        EmployeeJPA employee = new EmployeeJPA();
        employee.setName("Find Me");
        employee.setPosition("Tester");
        employee.setOffice("IT");
        employee.setExtn("E790");
        employee.setSalary("90000");
        employee.setStart_date(LocalDate.parse("2023-03-03", fmt));
        EmployeeJPA savedEmployee = employeeRepository.save(employee);

        // When
        EmployeeJPA foundEmployee = employeeService.getEmployeeById(savedEmployee.getId());

        // Then
        assertNotNull(foundEmployee);
        assertEquals(savedEmployee.getId(), foundEmployee.getId());
        assertEquals("Find Me", foundEmployee.getName());
    }

    @Test
    void getEmployeeById_whenEmployeeDoesNotExist_shouldReturnNull() {
        // When
        EmployeeJPA foundEmployee = employeeService.getEmployeeById("non-existent-id");

        // Then
        assertNull(foundEmployee);
    }

    @Test
    void updateEmployee_whenEmployeeExists_shouldReturnUpdatedEmployee() {
        // Given
        EmployeeJPA existingEmployee = new EmployeeJPA();
        existingEmployee.setName("Old Name");
        existingEmployee.setPosition("Developer");
        existingEmployee.setExtn("E791");
        existingEmployee.setSalary("90000");
        existingEmployee.setStart_date(LocalDate.parse("2023-03-03", fmt));
        existingEmployee.setOffice("IT");
        existingEmployee = employeeRepository.save(existingEmployee);

        EmployeeJPA employeeDetailsToUpdate = new EmployeeJPA();
        employeeDetailsToUpdate.setName("New Name");
        employeeDetailsToUpdate.setFirstName("New");
        employeeDetailsToUpdate.setLastName("Name");
        employeeDetailsToUpdate.setPosition("Manager");
        employeeDetailsToUpdate.setExtn("E792");
        employeeDetailsToUpdate.setSalary("100000");
        employeeDetailsToUpdate.setStart_date(LocalDate.parse("2023-04-04", fmt));
        employeeDetailsToUpdate.setOffice("Management");

        // When
        EmployeeJPA updatedEmployee = employeeService.updateEmployee(existingEmployee.getId(), employeeDetailsToUpdate, null);

        // Then
        assertNotNull(updatedEmployee);
        assertEquals(existingEmployee.getId(), updatedEmployee.getId());
        assertEquals("New Name", updatedEmployee.getName());
        assertEquals("Manager", updatedEmployee.getPosition());
    }

    @Test
    void updateEmployee_whenEmployeeDoesNotExist_shouldThrowException() {
        // Given
        EmployeeJPA employeeDetails = new EmployeeJPA();
        String nonExistentId = "non-existent-id";

        // When & Then
        // The service method now throws an exception instead of returning null.
        Exception exception = assertThrows(EntityNotFoundException.class, () -> {
            employeeService.updateEmployee(nonExistentId, employeeDetails, null);
        });

        assertTrue(exception.getMessage().contains("Employee not found with id: " + nonExistentId));
    }

    @Test
    void deleteEmployee_whenEmployeeExists_shouldReturnTrueAndRemoveEmployee() {
        // Given
        EmployeeJPA savedEmployee = employeeRepository.save(new EmployeeJPA());

        // When
        boolean deleted = employeeService.deleteEmployee(savedEmployee.getId());

        // Then
        assertTrue(deleted);
        assertFalse(employeeRepository.existsById(savedEmployee.getId()));
    }

    @Test
    void deleteEmployee_whenEmployeeDoesNotExist_shouldReturnFalse() {
        // When
        boolean deleted = employeeService.deleteEmployee("non-existent-id");

        // Then
        assertFalse(deleted);
    }

    @Test
    void deleteEmployee_whenManagerHasSubordinates_shouldThrowException() {
        // Given
        EmployeeJPA manager = new EmployeeJPA();
        manager.setName("Manager");
        employeeRepository.save(manager);

        EmployeeJPA subordinate = new EmployeeJPA();
        subordinate.setName("Subordinate");
        subordinate.setManager(manager);
        employeeRepository.save(subordinate);

        // When & Then
        Exception exception = assertThrows(IllegalStateException.class, () -> {
            employeeService.deleteEmployee(manager.getId());
        });

        assertTrue(exception.getMessage().contains("Cannot delete employee who is a manager with subordinates."));
        assertTrue(employeeRepository.existsById(manager.getId()), "Manager should not be deleted");
    }
}
