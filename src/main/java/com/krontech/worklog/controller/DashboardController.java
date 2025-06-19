    package com.krontech.worklog.controller;

    import com.krontech.worklog.dto.request.DashboardFilterRequest;
    import com.krontech.worklog.dto.response.DashboardResponse;
    import com.krontech.worklog.dto.response.QuickStatsResponse;
    import com.krontech.worklog.security.SecurityUtils;
    import com.krontech.worklog.service.DashboardService;
    import lombok.RequiredArgsConstructor;
    import lombok.extern.slf4j.Slf4j;
    import org.springframework.format.annotation.DateTimeFormat;
    import org.springframework.http.ResponseEntity;
    import org.springframework.security.access.prepost.PreAuthorize;
    import org.springframework.web.bind.annotation.*;

    import java.time.LocalDate;

    @RestController
    @RequestMapping("/api/dashboard")
    @RequiredArgsConstructor
    @Slf4j
    public class DashboardController {

        private final DashboardService dashboardService;

        /**
         * Get dashboard data for current user
         * Response varies based on user's role:
         * - Employee: Personal stats only
         * - Team Lead: Personal + team stats
         * - Director: Personal + department stats
         */
        @GetMapping
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<DashboardResponse> getDashboard(
                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                @RequestParam(required = false) String groupBy) {

            log.info("Getting dashboard for date range: {} to {}", startDate, endDate);

            DashboardFilterRequest filters = new DashboardFilterRequest();
            filters.setStartDate(startDate);
            filters.setEndDate(endDate);
            filters.setGroupBy(groupBy);

            Integer currentUserId = SecurityUtils.getCurrentUserId();
            return ResponseEntity.ok(dashboardService.getDashboard(currentUserId, filters));
        }

        /**
         * Get dashboard for specific employee
         * Only Team Leads (for their team) and Directors (for department) can access
         */
        @GetMapping("/employee/{employeeId}")
        @PreAuthorize("hasAnyRole('TEAM_LEAD', 'DIRECTOR')")
        public ResponseEntity<DashboardResponse> getEmployeeDashboard(
                @PathVariable Integer employeeId,
                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

            log.info("Getting dashboard for employee: {}", employeeId);

            DashboardFilterRequest filters = new DashboardFilterRequest();
            filters.setStartDate(startDate);
            filters.setEndDate(endDate);

            Integer currentUserId = SecurityUtils.getCurrentUserId();
            return ResponseEntity.ok(dashboardService.getEmployeeDashboard(currentUserId, employeeId, filters));
        }

        /**
         * Get quick statistics for header/widgets
         * - Today's hours
         * - Week total
         * - Pending tasks, etc.
         */
        @GetMapping("/stats/quick")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<QuickStatsResponse> getQuickStats() {
            log.info("Getting quick stats");
            Integer currentUserId = SecurityUtils.getCurrentUserId();
            return ResponseEntity.ok(dashboardService.getQuickStats(currentUserId));
        }

        /**
         * Get team dashboard (Team Lead and Director)
         */
        @GetMapping("/team")
        @PreAuthorize("hasAnyRole('TEAM_LEAD', 'DIRECTOR')")
        public ResponseEntity<DashboardResponse> getTeamDashboard(
                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                @RequestParam(required = false) Integer teamLeadId) {

            log.info("Getting team dashboard");

            DashboardFilterRequest filters = new DashboardFilterRequest();
            filters.setStartDate(startDate);
            filters.setEndDate(endDate);
            filters.setTeamLeadId(teamLeadId);

            Integer currentUserId = SecurityUtils.getCurrentUserId();
            return ResponseEntity.ok(dashboardService.getDashboard(currentUserId, filters));
        }
    }