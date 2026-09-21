package com.angular.backend.employees;

import java.time.LocalDate;

public record EmployeeRestoreRequest(
        String id,
        String name,
        String firstName,
        String lastName,
        String position,
        String extn,
        String salary,
        LocalDate start_date,
        String office,
        boolean hasManagerRights,
        String managerId) {
}
