package com.krontech.worklog.controller;

import com.krontech.worklog.dto.response.WorklogTypeResponse;
import com.krontech.worklog.service.WorklogTypeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/worklog-types")
@RequiredArgsConstructor
@Slf4j
public class WorklogTypeController {

    private final WorklogTypeService worklogTypeService;

    /**
     * Get all active worklog types
     * Used in dropdown menus when creating worklogs
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<WorklogTypeResponse>> getActiveWorklogTypes() {
        log.info("Getting all active worklog types");
        return ResponseEntity.ok(worklogTypeService.getActiveWorklogTypes());
    }

    /**
     * Get specific worklog type details
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<WorklogTypeResponse> getWorklogType(@PathVariable Integer id) {
        log.info("Getting worklog type: {}", id);
        return ResponseEntity.ok(worklogTypeService.getWorklogType(id));
    }
}