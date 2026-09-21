package com.angular.backend.employees;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EmployeeCsvExportServiceTest {

	@TempDir
	Path exportDirectory;

	@Test
	void recordChangeIncludesPositionUpdates() throws IOException {
		EmployeeCsvExportService service = new EmployeeCsvExportService(mock(EmployeeRepository.class),
				exportDirectory.toString(), "intra_day_changes.csv", "complete_employees.csv", "Europe/Prague");
		EmployeeJPA oldEmployee = employee("employee-2", "50000");
		EmployeeJPA newEmployee = employee("employee-2", "50000");
		oldEmployee.setPosition("Developer");
		newEmployee.setPosition("Senior Developer");

		service.recordChange("UPDATE", oldEmployee, newEmployee);

		String changes = service.readIntradayChanges();

		assertTrue(changes.contains("\"Developer\",\"Senior Developer\""));
	}

	@Test
	void readIntradayChangesContainsOnlyUpdatesAfterCompleteEmployeeExport() throws IOException {
		EmployeeRepository employeeRepository = mock(EmployeeRepository.class);
		when(employeeRepository.findAllWithManagers()).thenReturn(List.of());
		EmployeeCsvExportService service = new EmployeeCsvExportService(employeeRepository,
				exportDirectory.toString(), "intra_day_changes.csv", "complete_employees.csv", "Europe/Prague");
		EmployeeJPA oldEmployee = employee("employee-1", "50000");
		EmployeeJPA newEmployee = employee("employee-1", "60000");

		service.recordChange("UPDATE", oldEmployee, employee("employee-1", "55000"));
		service.exportCompleteEmployees();

		String exportDate = LocalDate.now(ZoneId.of("Europe/Prague"))
				.format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy"));
		assertTrue(java.nio.file.Files.exists(exportDirectory.resolve("complete_employees_" + exportDate + ".csv")));
		assertTrue(java.nio.file.Files.readString(exportDirectory.resolve("intra_day_changes_" + exportDate + ".csv"))
				.contains("\"50000\",\"55000\""));

		service.recordChange("UPDATE", oldEmployee, newEmployee);

		String changes = service.readIntradayChanges();

		assertTrue(changes.contains("\"50000\",\"60000\""));
		org.junit.jupiter.api.Assertions.assertFalse(changes.contains("\"50000\",\"55000\""));
	}

	private EmployeeJPA employee(String id, String salary) {
		EmployeeJPA employee = new EmployeeJPA();
		employee.setId(id);
		employee.setName("Employee " + id);
		employee.setSalary(salary);
		return employee;
	}
}
