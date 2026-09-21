package com.angular.backend.employees;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class TreeBuilder {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private ObjectMapper objectMapper;

    public List<EmployeeJPA> buildTree() {
        // Fetch all employees and their managers in an efficient query to avoid N+1 problem.
        List<EmployeeJPA> employees = employeeRepository.findAllWithManagers();

        // Use a map for quick lookups by employee ID.
        Map<String, EmployeeJPA> employeeMap = new HashMap<>();
        for (EmployeeJPA emp : employees) {
            employeeMap.put(emp.getId(), emp);
        }

        // Build the tree structure by linking children to their parents.
        List<EmployeeJPA> rootNodes = new ArrayList<>();
        for (EmployeeJPA employee : employees) {
            EmployeeJPA manager = employee.getManager();
            if (manager != null) {
                // The manager is guaranteed to be in the map because we fetched all employees.
                employeeMap.get(manager.getId()).addChild(employee);
            } else {
                // Employees without a manager are root nodes.
                rootNodes.add(employee);
            }
        }
        return rootNodes;
    }

    /**
     * Builds the employee tree and returns it as a JSON string.
     *
     * @return A JSON representation of the employee hierarchy.
     * @throws JsonProcessingException if there is an error during
     * serialization.
     */
    public String getTreeAsJson() throws JsonProcessingException {
        List<EmployeeJPA> rootNodes = buildTree();
        return objectMapper.writeValueAsString(rootNodes);
    }
}
