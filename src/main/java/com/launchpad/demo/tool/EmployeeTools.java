package com.launchpad.demo.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Demo point 5: Tool chaining.
 *
 * Two independent @Tool methods over the EMS Employee domain. Ask something
 * like "What is the leave balance for employee E102?" and the model has to
 * call getEmployee() first to confirm the employee exists / get context,
 * then call getLeaveBalance() - a genuine two-hop tool chain in one turn,
 * not two separate requests.
 *
 * Backed by an in-memory map here purely to keep the demo self-contained;
 * swap for the real EmployeeRepository/JPA lookup in the actual EMS app.
 */
@Component
public class EmployeeTools {

    private static final Map<String, Map<String, Object>> EMPLOYEES = Map.of(
            "E101", Map.of("name", "Ananya Rao", "department", "Engineering", "leaveBalance", 12),
            "E102", Map.of("name", "Rahul Verma", "department", "Sales", "leaveBalance", 5),
            "E103", Map.of("name", "Priya Nair", "department", "HR", "leaveBalance", 18)
    );

    @Tool(description = "Look up an employee's basic profile (name and department) by employee ID.")
    public String getEmployee(String employeeId) {
        Map<String, Object> employee = EMPLOYEES.get(employeeId.toUpperCase());
        if (employee == null) {
            return "No employee found with ID " + employeeId;
        }
        return "Employee %s: %s, Department: %s".formatted(
                employeeId, employee.get("name"), employee.get("department"));
    }

    @Tool(description = "Get the remaining annual leave balance (in days) for an employee by employee ID.")
    public String getLeaveBalance(String employeeId) {
        Map<String, Object> employee = EMPLOYEES.get(employeeId.toUpperCase());
        if (employee == null) {
            return "No employee found with ID " + employeeId;
        }
        return "Employee %s has %s leave days remaining".formatted(employeeId, employee.get("leaveBalance"));
    }
}
