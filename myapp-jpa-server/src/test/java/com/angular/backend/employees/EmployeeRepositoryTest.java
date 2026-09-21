package com.angular.backend.employees;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.angular.backend.AbstractIntegrationTest;

@Testcontainers
@DataJpaTest
public class EmployeeRepositoryTest extends AbstractIntegrationTest {

    private static final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    private EmployeeRepository employeeRepository;

    @Test
    void testCreateAndFindEmployee() {
        EmployeeJPA employee = new EmployeeJPA();
        employee.setName("Test Employee");
        employee.setPosition("Developer");
        employee.setExtn("1234");
        employee.setSalary("100000");
        employee.setStart_date(LocalDate.parse("2024-10-24", fmt));
        employee.setOffice("New York");

        EmployeeJPA saved = employeeRepository.save(employee);
        assertNotNull(saved.getId());

        Optional<EmployeeJPA> found = employeeRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("Test Employee", found.get().getName());
        assertEquals("Developer", found.get().getPosition());
        assertEquals("1234", found.get().getExtn());
        assertEquals("100000", found.get().getSalary());
        assertEquals(LocalDate.parse("2024-10-24", fmt), found.get().getStart_date());
        assertEquals("New York", found.get().getOffice());
    }

    @Test
    void testEmployeeNamesMustBeUnique() {
        EmployeeJPA firstEmployee = new EmployeeJPA();
        firstEmployee.setName("Unique Name");
        employeeRepository.saveAndFlush(firstEmployee);

        EmployeeJPA duplicateEmployee = new EmployeeJPA();
        duplicateEmployee.setName("Unique Name");

        assertThrows(RuntimeException.class, () -> employeeRepository.saveAndFlush(duplicateEmployee));
    }

    @Test
    void testDeleteEmployee() {
        EmployeeJPA employee = new EmployeeJPA();
        employee.setName("Delete Me");
        employee.setPosition("Tester");
        employee.setExtn("5678");
        employee.setSalary("80000");
        employee.setStart_date(LocalDate.parse("2024-10-24", fmt));
        employee.setOffice("Los Angeles");

        EmployeeJPA saved = employeeRepository.save(employee);
        String id = saved.getId();
        employeeRepository.deleteById(id);
        assertFalse(employeeRepository.findById(id).isPresent());
    }

    @Test
    void testDeleteManagerWithSubordinates_shouldThrowException() {
        // Given
        EmployeeJPA manager = new EmployeeJPA();
        manager.setName("Manager");
        employeeRepository.save(manager);

        EmployeeJPA employee = new EmployeeJPA();
        employee.setName("Subordinate");
        employee.setManager(manager);
        employeeRepository.save(employee);

        // When & Then
        // Deleting a manager with subordinates should violate a foreign key constraint.
        // We need to flush to trigger the exception inside the test method.
        assertThrows(RuntimeException.class, () -> {
            employeeRepository.deleteById(manager.getId());
            employeeRepository.flush();
        });
    }

    @Test
    void testExistsByManagerId() {
        // Given
        EmployeeJPA manager = new EmployeeJPA();
        manager.setName("Manager");
        employeeRepository.save(manager);

        EmployeeJPA employee = new EmployeeJPA();
        employee.setName("Subordinate");
        employee.setManager(manager);
        employeeRepository.save(employee);

        EmployeeJPA employeeWithoutManager = new EmployeeJPA();
        employeeWithoutManager.setName("No Manager");
        employeeRepository.save(employeeWithoutManager);

        // When & Then
        assertTrue(employeeRepository.existsByManagerId(manager.getId()));
        assertFalse(employeeRepository.existsByManagerId(employee.getId()));
        assertFalse(employeeRepository.existsByManagerId("non-existent-id"));
    }
}
