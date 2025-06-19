package com.krontech.worklog.controller;

import com.krontech.worklog.dto.response.DepartmentDetailsResponse;
import com.krontech.worklog.dto.response.DepartmentHierarchyResponse;
import com.krontech.worklog.dto.response.DepartmentSummaryResponse;
import com.krontech.worklog.dto.response.UserDepartmentResponse;
import com.krontech.worklog.security.SecurityUtils;
import com.krontech.worklog.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<List<DepartmentSummaryResponse>> getAllDepartments() {
        log.info("Getting all departments with stats");
        return ResponseEntity.ok(departmentService.getAllDepartmentsWithStats());
    }

    /**
     * Get specific department details
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('DIRECTOR')")
    public ResponseEntity<DepartmentDetailsResponse> getDepartment(@PathVariable Integer id) {
        log.info("Getting department: {}", id);
        return ResponseEntity.ok(departmentService.getDepartmentDetails(id));
    }

    /**
     * Get department hierarchy visualization
     * Shows director -> team leads -> employees structure
     */
    @GetMapping("/{id}/hierarchy")
    @PreAuthorize("hasRole('DIRECTOR')")
    public ResponseEntity<DepartmentHierarchyResponse> getDepartmentHierarchy(@PathVariable Integer id) {
        log.info("Getting department hierarchy for: {}", id);
        return ResponseEntity.ok(departmentService.getDepartmentHierarchy(id));
    }

    /**
     * Get current user's department info
     */
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDepartmentResponse> getMyDepartment() {
        log.info("Getting current user's department");
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(departmentService.getUserDepartment(currentUserId));
    }
}