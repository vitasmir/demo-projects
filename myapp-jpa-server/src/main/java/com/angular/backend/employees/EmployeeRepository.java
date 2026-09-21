package com.angular.backend.employees;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EmployeeRepository extends JpaRepository<EmployeeJPA, String> {

    @Modifying
    @Query(value = "TRUNCATE TABLE employees RESTART IDENTITY CASCADE", nativeQuery = true)
    void truncateEmployees();

        @Modifying
        @Query(value = "INSERT INTO employees (id, name, first_name, last_name, position, extn, salary, start_date, office, has_manager_rights, manager_id) "
            + "VALUES (:id, :name, :firstName, :lastName, :position, :extn, :salary, :startDate, :office, :hasManagerRights, NULL)", nativeQuery = true)
        void restoreEmployee(String id, String name, String firstName, String lastName, String position, String extn, String salary,
            java.time.LocalDate startDate, String office, boolean hasManagerRights);

        @Modifying
        @Query(value = "UPDATE employees SET manager_id = :managerId WHERE id = :id", nativeQuery = true)
        void restoreEmployeeManager(String id, String managerId);

    @Query("SELECT e FROM EmployeeJPA e LEFT JOIN FETCH e.manager")
    List<EmployeeJPA> findAllWithManagers();

    @Query("SELECT e FROM EmployeeJPA e LEFT JOIN FETCH e.manager WHERE e.id = :id")
    EmployeeJPA findByIdWithManager(@Param("id") String id);

    /**
     * Finds all employees who can be assigned as a manager. This includes root
     * employees (those without a manager) and any employee who has the
     * 'hasManagerRights' flag set to true.
     */
    @Query("SELECT e FROM EmployeeJPA e WHERE e.hasManagerRights = true")
    List<EmployeeJPA> findPotentialManagers();

    /**
     * Checks if any employee is managed by the given manager ID.
     *
     * @param managerId The ID of the manager to check for.
     * @return true if at least one employee has this manager, false otherwise.
     */
    boolean existsByManagerId(String managerId);

    List<EmployeeJPA> findByManagerId(String managerId);

    boolean existsByName(String name);

}
