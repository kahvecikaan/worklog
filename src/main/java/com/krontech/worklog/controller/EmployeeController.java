package com.krontech.worklog.controller;

import com.krontech.worklog.dto.response.EmployeeResponse;
import com.krontech.worklog.security.SecurityUtils;
import com.krontech.worklog.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
@Slf4j
public class EmployeeController {

    private final EmployeeService employeeService;

    /**
     * Get current logged-in user's employee profile
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EmployeeResponse> getCurrentEmployee() {
        log.info("Getting current employee profile");
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(employeeService.getEmployee(currentUserId));
    }

    /**
     * Get all employees visible to current user
     * - Employees see only themselves
     * - Team Leads see their team members
     * - Directors see entire department
     */
    @GetMapping("/visible")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<EmployeeResponse>> getVisibleEmployees() {
        log.info("Getting visible employees for current user");
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(employeeService.getEmployeesVisibleToUser(currentUserId));
    }

    /**
     * Get specific employee details (if authorized)
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EmployeeResponse> getEmployee(@PathVariable Integer id) {
        log.info("Getting employee details for ID: {}", id);
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(employeeService.getEmployeeIfAuthorized(currentUserId, id));
    }

    /**
     * Get all employees in department (Director only)
     */
    @GetMapping("/department")
    @PreAuthorize("hasRole('DIRECTOR')")
    public ResponseEntity<List<EmployeeResponse>> getDepartmentEmployees() {
        log.info("Director getting all department employees");
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(employeeService.getDepartmentEmployees(currentUserId));
    }

    /**
     * Get employees by team lead (Team Lead and Director)
     */
    @GetMapping("/team/{teamLeadId}")
    @PreAuthorize("hasAnyRole('TEAM_LEAD', 'DIRECTOR')")
    public ResponseEntity<List<EmployeeResponse>> getTeamMembers(@PathVariable Integer teamLeadId) {
        log.info("Getting team members for team lead: {}", teamLeadId);
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        // Service will verify if current user can view this team
        return ResponseEntity.ok(employeeService.getTeamMembers(currentUserId, teamLeadId));
    }
}