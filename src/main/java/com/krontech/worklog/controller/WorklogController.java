package com.krontech.worklog.controller;

import com.krontech.worklog.dto.request.WorklogCreateRequest;
import com.krontech.worklog.dto.request.WorklogUpdateRequest;
import com.krontech.worklog.dto.response.WorklogResponse;
import com.krontech.worklog.security.SecurityUtils;
import com.krontech.worklog.service.WorklogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/worklogs")
@RequiredArgsConstructor
@Slf4j
public class WorklogController {

    private final WorklogService worklogService;

    @PostMapping
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'TEAM_LEAD', 'DIRECTOR')")
    public ResponseEntity<WorklogResponse> createWorklog(@Valid @RequestBody WorklogCreateRequest request) {
        log.info("Creating worklog for date: {}", request.getWorkDate());
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        WorklogResponse response = worklogService.createWorklog(currentUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<WorklogResponse>> getMyWorklogs(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("Getting my worklogs from {} to {}", startDate, endDate);
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(worklogService.getEmployeeWorklogs(currentUserId, startDate, endDate));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<WorklogResponse> getWorklogById(@PathVariable Integer id) {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(worklogService.getWorklogById(id, currentUserId));
    }

    // Get worklogs for a specific date
    @GetMapping("/my/date/{date}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<WorklogResponse>> getWorklogsForDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        log.info("Getting worklogs for date: {}", date);
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(worklogService.getWorklogsForDate(currentUserId, date));
    }

    // Update worklog
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<WorklogResponse> updateWorklog(
            @PathVariable Integer id,
            @Valid @RequestBody WorklogUpdateRequest request) {
        log.info("Updating worklog: {}", id);
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(worklogService.updateWorklog(id, currentUserId, request));
    }

    // Delete worklog
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteWorklog(@PathVariable Integer id) {
        log.info("Deleting worklog: {}", id);
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        worklogService.deleteWorklog(id, currentUserId);
    }

    // Get team worklogs (Team Lead and Director)
    @GetMapping("/team")
    @PreAuthorize("hasAnyRole('TEAM_LEAD', 'DIRECTOR')")
    public ResponseEntity<List<WorklogResponse>> getTeamWorklogs(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer employeeId) {
        log.info("Getting team worklogs from {} to {}", startDate, endDate);
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(worklogService.getTeamWorklogs(currentUserId, startDate, endDate, employeeId));
    }

    // Get department worklogs (Director only)
    @GetMapping("/department")
    @PreAuthorize("hasRole('DIRECTOR')")
    public ResponseEntity<List<WorklogResponse>> getDepartmentWorklogs(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer teamLeadId,
            @RequestParam(required = false) Integer employeeId) {
        log.info("Getting department worklogs from {} to {}", startDate, endDate);
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(worklogService.getDepartmentWorklogs(
                currentUserId, startDate, endDate, teamLeadId, employeeId));
    }
}