package com.krontech.worklog.controller;

import com.krontech.worklog.security.SecurityUtils;
import com.krontech.worklog.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
@Slf4j
public class DepartmentController {

    private final DepartmentService departmentService;

    /**
     * Get all departments with basic statistics
     * Director only endpoint
     */
    @GetMapping
    @PreAuthorize("hasRole('DIRECTOR')")
    public ResponseEntity<List<Map<String, Object>>> getAllDepartments() {
        log.info("Getting all departments with stats");
        return ResponseEntity.ok(departmentService.getAllDepartmentsWithStats());
    }

    /**
     * Get specific department details
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('DIRECTOR')")
    public ResponseEntity<?> getDepartment(@PathVariable Integer id) {
        log.info("Getting department: {}", id);
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(departmentService.getDepartmentDetails(id, currentUserId));
    }

    /**
     * Get department hierarchy visualization
     * Shows director -> team leads -> employees structure
     */
    @GetMapping("/{id}/hierarchy")
    @PreAuthorize("hasRole('DIRECTOR')")
    public ResponseEntity<Map<String, Object>> getDepartmentHierarchy(@PathVariable Integer id) {
        log.info("Getting department hierarchy for: {}", id);
        return ResponseEntity.ok(departmentService.getDepartmentHierarchy(id));
    }

    /**
     * Get current user's department info
     */
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getMyDepartment() {
        log.info("Getting current user's department");
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(departmentService.getUserDepartment(currentUserId));
    }
}