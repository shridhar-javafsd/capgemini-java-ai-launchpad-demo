package com.launchpad.demo.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class EmsTools {

    private static final Map<String, Map<String, Object>> EMPLOYEES = Map.of(
            "E101", Map.of("name", "Ananya Rao", "department", "Engineering", "leaveBalance", 12),
            "E102", Map.of("name", "Rahul Verma", "department", "Sales", "leaveBalance", 5),
            "E103", Map.of("name", "Priya Nair", "department", "HR", "leaveBalance", 18)
    );

    private final RestClient tavily = RestClient.create("https://api.tavily.com");

    @Value("${TAVILY_API_KEY:}")
    private String tavilyApiKey;

    @Tool(description = "Look up an employee's basic profile (name and department) by employee ID.")
    public String getEmployee(String employeeId) {
        var employee = EMPLOYEES.get(employeeId.toUpperCase());
        if (employee == null) return "No employee found with ID " + employeeId;
        return "Employee %s: %s, Department: %s".formatted(employeeId, employee.get("name"), employee.get("department"));
    }

    @Tool(description = "Get the remaining annual leave balance in days for an employee by employee ID. "
            + "Requires looking up the employee first if their existence isn't already confirmed.")
    public String getLeaveBalance(String employeeId) {
        var employee = EMPLOYEES.get(employeeId.toUpperCase());
        if (employee == null) return "No employee found with ID " + employeeId;
        return "Employee %s has %s leave days remaining".formatted(employeeId, employee.get("leaveBalance"));
    }

    @Tool(description = "Search the public web for current information not in the model's training data, "
            + "such as recent news or fast-changing facts.")
    public String searchWeb(String query) {
        if (tavilyApiKey.isBlank()) {
            return "Web search is not configured (no TAVILY_API_KEY set). Would have searched for: " + query;
        }
        return tavily.post()
                .uri("/search")
                .body(Map.of("api_key", tavilyApiKey, "query", query, "max_results", 3))
                .retrieve()
                .body(String.class);
    }
}
