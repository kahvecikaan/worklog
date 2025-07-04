package com.krontech.worklog.service;

import com.krontech.worklog.dto.projection.*;
import com.krontech.worklog.dto.request.DashboardFilterRequest;
import com.krontech.worklog.dto.response.DashboardResponse;
import com.krontech.worklog.dto.response.DashboardResponse.*;
import com.krontech.worklog.dto.response.QuickStatsResponse;
import com.krontech.worklog.entity.Employee;
import com.krontech.worklog.entity.Role;
import com.krontech.worklog.entity.Worklog;
import com.krontech.worklog.repository.EmployeeRepository;
import com.krontech.worklog.repository.WorklogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class DashboardService {

    private final EmployeeRepository employeeRepository;
    private final WorklogRepository worklogRepository;

    public DashboardResponse getDashboard(Integer employeeId, DashboardFilterRequest filters) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        // Set default date range if not provided (current week)
        if (filters.getStartDate() == null) {
            filters.setStartDate(LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)));
        }
        if (filters.getEndDate() == null) {
            filters.setEndDate(LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)));
        }

        DashboardResponse.DashboardResponseBuilder responseBuilder = DashboardResponse.builder();

        // Build current user summary
        responseBuilder.currentUser(EmployeeSummary.builder()
                .id(employee.getId())
                .name(employee.getFullName())
                .role(employee.getRole().getDisplayName())
                .department(employee.getDepartment().getName())
                .build());

        // Determine which dashboard to show based on role
        switch (employee.getRole()) {
            case EMPLOYEE:
                buildEmployeeDashboard(employee, filters, responseBuilder);
                break;
            case TEAM_LEAD:
                buildTeamLeadDashboard(employee, filters, responseBuilder);
                break;
            case DIRECTOR:
                buildDirectorDashboard(employee, filters, responseBuilder);
                break;
        }

        return responseBuilder.build();
    }

    private void buildEmployeeDashboard(Employee employee, DashboardFilterRequest filters,
                                        DashboardResponse.DashboardResponseBuilder responseBuilder) {
        LocalDate startDate = filters.getStartDate();
        LocalDate endDate = filters.getEndDate();

        // Get total hours
        Integer totalHours = worklogRepository.getTotalHoursByEmployee(employee.getId(), startDate, endDate);
        if (totalHours == null) totalHours = 0;

        // Calculate working days in period
        long totalDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        long weekends = calculateWeekends(startDate, endDate);
        long workingDays = totalDays - weekends;

        // Get days actually worked
        List<Worklog> worklogs = worklogRepository.findByEmployeeIdAndWorkDateBetweenOrderByWorkDateDesc(
                employee.getId(), startDate, endDate
        );
        long daysWorked = worklogs.stream().map(Worklog::getWorkDate).distinct().count();

        // Build period summary
        responseBuilder.periodSummary(PeriodSummary.builder()
                        .totalHours(totalHours)
                        .totalDays(totalHours / 8.0)
                        .daysWorked((int) daysWorked)
                        .averageHoursPerDay(daysWorked > 0 ? (double) totalHours / daysWorked : 0.0)
                        .startDate(startDate)
                        .endDate(endDate)
                        .build());

        // Get worklog type breakdown
        List<WorklogTypeHoursProjection> typeBreakdown = worklogRepository.getHoursByTypeForEmployee(
                employee.getId(), startDate, endDate
        );

        List<WorklogTypeBreakdown> breakdowns = new ArrayList<>();
        for (WorklogTypeHoursProjection projection : typeBreakdown) {
            breakdowns.add(WorklogTypeBreakdown.builder()
                            .typeName(projection.getTypeName())
                            .hours(projection.getHours().intValue())
                            .percentage(totalHours > 0 ? (projection.getHours() * 100.0) / totalHours : 0.0)
                            .build());
        }
        responseBuilder.worklogTypeBreakdown(breakdowns);

        // Get recent worklogs
        List<RecentWorklog> recentLogs = worklogs.stream()
                .limit(5)
                .map(w -> RecentWorklog.builder()
                        .date(w.getWorkDate())
                        .type(w.getWorklogType().getName())
                        .hours(w.getHoursWorked())
                        .description(w.getDescription())
                        .projectName(w.getProjectName())
                        .build())
                .collect(Collectors.toList());
        responseBuilder.recentWorklogs(recentLogs);
    }

    private void buildTeamLeadDashboard(Employee teamLead, DashboardFilterRequest filters,
                                        DashboardResponse.DashboardResponseBuilder responseBuilder) {
        // First, build the team lead's own dashboard
        buildEmployeeDashboard(teamLead, filters, responseBuilder);

        LocalDate startDate = filters.getStartDate();
        LocalDate endDate = filters.getEndDate();

        // Get team members summary
        List<TeamMemberProjection> teamSummaryData = worklogRepository.getTeamSummary(
                teamLead.getId(), startDate, endDate
        );

        List<TeamMemberSummary> teamMembers = new ArrayList<>();
        int totalTeamHours = 0;
        int membersWithLogs = 0;

        // Calculate working days for utilization
        long workingDays = calculateWorkingDays(startDate, endDate);

        for (TeamMemberProjection projection : teamSummaryData) {
            // Direct, type-safe access to fields - no casting needed!
            Employee member = employeeRepository.findById(projection.getId()).orElse(null);
            if (member == null) continue;

            // Track members who actually logged work
            if (projection.getTotalHours() > 0) {
                membersWithLogs++;
            }

            double utilizationRate = (workingDays > 0) ?
                    (projection.getTotalHours() * 100.0) / (workingDays * 8) : 0.0;

            teamMembers.add(TeamMemberSummary.builder()
                            .id(projection.getId())
                            .name(projection.getFirstName() + " " + projection.getLastName())
                            .grade(member.getGrade().getTitle())
                            .totalHours(projection.getTotalHours().intValue())
                            .daysWorked(projection.getDaysWorked().intValue())
                            .utilizationRate(utilizationRate)
                            .build());

            totalTeamHours += projection.getTotalHours().intValue();
        }

        responseBuilder.teamMembers(teamMembers);

        // Calculate team statistics
        if (!teamMembers.isEmpty()) {
            double avgHoursPerMember = (double) totalTeamHours / teamMembers.size();
            double teamUtilization = (workingDays > 0) ?
                    (totalTeamHours * 100.0) / (teamMembers.size() * workingDays * 8) : 0.0;

            responseBuilder.teamStats(TeamStatistics.builder()
                            .teamSize(teamMembers.size())
                            .totalTeamHours(totalTeamHours)
                            .averageHoursPerMember(avgHoursPerMember)
                            .teamUtilizationRate(teamUtilization)
                            .build());

            log.info("Team lead dashboard - Team size: {}, Members with logs: {}, Total hours: {}, Utilization: {}%",
                    teamMembers.size(), membersWithLogs, totalTeamHours, teamUtilization);
        }
    }

    private void buildDirectorDashboard(Employee director, DashboardFilterRequest filters,
                                        DashboardResponse.DashboardResponseBuilder responseBuilder) {
        // First, build the director's own dashboard
        buildEmployeeDashboard(director, filters, responseBuilder);

        LocalDate startDate = filters.getStartDate();
        LocalDate endDate = filters.getEndDate();
        Integer departmentId = director.getDepartment().getId();

        // Get all employees in department
        List<Employee> departmentEmployees = employeeRepository.findByDepartmentIdAndIsActiveTrue(departmentId);
        List<Employee> teamLeads = departmentEmployees.stream()
                .filter(e -> e.getRole() == Role.TEAM_LEAD)
                .toList();

        // Build team leads summary with enhanced metrics
        List<TeamLeadSummary> teamLeadSummaries = new ArrayList<>();
        int departmentTotalHours = 0;
        long workingDays = calculateWorkingDays(startDate, endDate);

        // Track best and worst performing teams
        TeamLeadSummary bestPerformingTeam = null;
        TeamLeadSummary worstPerformingTeam = null;
        double highestUtilization = 0;
        double lowestUtilization = 100;

        // Use the new projection-based method for department team summary
        List<DepartmentTeamSummaryProjection> teamSummaries =
                worklogRepository.getDepartmentTeamSummary(departmentId, startDate, endDate);

        // Create a map for quick lookup
        Map<Integer, DepartmentTeamSummaryProjection> teamDataMap = teamSummaries.stream()
                .collect(Collectors.toMap(DepartmentTeamSummaryProjection::getTeamLeadId, ts -> ts));

        for (Employee teamLead : teamLeads) {
            // Get team lead's own hours first
            Integer teamLeadHours = worklogRepository.getTotalHoursByEmployee(
                    teamLead.getId(), startDate, endDate
            );
            if (teamLeadHours == null) teamLeadHours = 0;

            // Get team data from projection
            DepartmentTeamSummaryProjection teamData = teamDataMap.get(teamLead.getId());

            int teamSize = teamData != null ? teamData.getTeamSize().intValue() : 0;
            int teamMembersHours = teamData != null ? teamData.getTotalHours().intValue() : 0;

            // Calculate members with logs (need additional query or tracking)
            List<TeamMemberProjection> teamMemberData = worklogRepository.getTeamSummary(
                    teamLead.getId(), startDate, endDate
            );
            int teamMembersWithLogs = (int) teamMemberData.stream()
                    .filter(m -> m.getTotalHours() > 0)
                    .count();

            // Total team hours = team lead hours + team members hours
            int totalTeamHours = teamLeadHours + teamMembersHours;

            // Utilization includes the team lead in calculation
            double teamUtilization = (workingDays > 0 && (teamSize + 1) > 0) ?
                    (totalTeamHours * 100.0) / ((teamSize + 1) * workingDays * 8) : 0.0;

            TeamLeadSummary summary = TeamLeadSummary.builder()
                    .id(teamLead.getId())
                    .name(teamLead.getFullName())
                    .teamSize(teamSize) // Just team members count
                    .teamTotalHours(totalTeamHours) // Includes team lead
                    .teamUtilizationRate(teamUtilization)
                    .teamMembersWithLogs(teamMembersWithLogs)
                    .build();

            teamLeadSummaries.add(summary);

            // Track best and worst performing teams
            if (teamUtilization > highestUtilization) {
                highestUtilization = teamUtilization;
                bestPerformingTeam = summary;
            }
            if (teamUtilization < lowestUtilization) {
                lowestUtilization = teamUtilization;
                worstPerformingTeam = summary;
            }

            departmentTotalHours += totalTeamHours;
        }

        // Add director's own hours to department total
        Integer directorHours = worklogRepository.getTotalHoursByEmployee(
                director.getId(), startDate, endDate
        );
        if (directorHours == null) directorHours = 0;
        departmentTotalHours += directorHours;

        // Add hours from employees who report directly to director (if any)
        List<Employee> directReports = departmentEmployees.stream()
                .filter(e -> e.getRole() == Role.EMPLOYEE &&
                        (e.getTeamLead() == null || e.getTeamLead().getId().equals(director.getId())))
                .toList();

        for (Employee directReport : directReports) {
            Integer empHours = worklogRepository.getTotalHoursByEmployee(
                    directReport.getId(), startDate, endDate
            );
            if (empHours != null) {
                departmentTotalHours += empHours;
            }
        }

        responseBuilder.teamLeads(teamLeadSummaries);

        // Add team performance insights
        if (bestPerformingTeam != null && worstPerformingTeam != null) {
            responseBuilder.teamPerformanceInsights(TeamPerformanceInsights.builder()
                            .bestPerformingTeamId(bestPerformingTeam.getId())
                            .bestPerformingTeamName(bestPerformingTeam.getName())
                            .bestPerformingTeamUtilization(bestPerformingTeam.getTeamUtilizationRate())
                            .worstPerformingTeamId(worstPerformingTeam.getId())
                            .worstPerformingTeamName(worstPerformingTeam.getName())
                            .worstPerformingTeamUtilization(worstPerformingTeam.getTeamUtilizationRate())
                            .utilizationGap(highestUtilization - lowestUtilization)
                            .build());
        }

        // Get department-wide worklog type breakdown
        List<WorklogTypeHoursProjection> deptTypeBreakdown =
                worklogRepository.getDepartmentWorklogTypeSummary(departmentId, startDate, endDate);

        List<WorklogTypeBreakdown> deptBreakdowns = new ArrayList<>();
        int actualDeptTotal = 0;

        for (WorklogTypeHoursProjection projection : deptTypeBreakdown) {
            actualDeptTotal += projection.getHours().intValue();
            deptBreakdowns.add(WorklogTypeBreakdown.builder()
                            .typeName(projection.getTypeName())
                            .hours(projection.getHours().intValue())
                            .percentage(0.0) // Will calculate after getting total
                            .build());
        }

        // Update percentages with actual total
        if (actualDeptTotal > 0) {
            for (WorklogTypeBreakdown breakdown : deptBreakdowns) {
                breakdown.setPercentage((breakdown.getHours() * 100.0) / actualDeptTotal);
            }
        }

        departmentTotalHours = actualDeptTotal;

        // Only set department breakdown if we have data
        if (!deptBreakdowns.isEmpty()) {
            responseBuilder.worklogTypeBreakdown(deptBreakdowns);
        }

        // Calculate department statistics
        int totalEmployees = departmentEmployees.size() - 1; // Exclude director
        double deptUtilization = (workingDays > 0 && totalEmployees > 0) ?
                (departmentTotalHours * 100.0) / (totalEmployees * workingDays * 8) : 0.0;

        // Count employees who have logged work
        int employeesWithLogs = 0;
        for (Employee emp : departmentEmployees) {
            if (!emp.getId().equals(director.getId())) {
                Integer empHours = worklogRepository.getTotalHoursByEmployee(
                        emp.getId(), startDate, endDate
                );
                if (empHours != null && empHours > 0) {
                    employeesWithLogs++;
                }
            }
        }

        double logComplianceRate = (totalEmployees > 0) ?
                (employeesWithLogs * 100.0 / totalEmployees) : 0.0;

        responseBuilder.departmentStats(DepartmentStatistics.builder()
                        .totalEmployees(totalEmployees)
                        .totalTeamLeads(teamLeads.size())
                        .departmentTotalHours(departmentTotalHours)
                        .departmentUtilizationRate(deptUtilization)
                        .employeesWithLogs(employeesWithLogs)
                        .logComplianceRate(logComplianceRate)
                        .build());

        log.info("Director dashboard - Department total hours: {}, Total employees: {}, Utilization: {}%, Compliance: {}%",
                departmentTotalHours, totalEmployees, deptUtilization,
                logComplianceRate);
    }

    public DashboardResponse getEmployeeDashboard(Integer currentUserId, Integer targetEmployeeId,
                                                  DashboardFilterRequest filters) {
        Employee currentUser = employeeRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Employee targetEmployee = employeeRepository.findById(targetEmployeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        // Verify authorization
        if (!currentUser.canViewEmployee(targetEmployee)) {
            throw new RuntimeException("You don't have permission to view this employee's dashboard");
        }

        // Build dashboard for target employee
        return getDashboard(targetEmployeeId, filters);
    }

    public QuickStatsResponse getQuickStats(Integer employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        // Current week
        LocalDate weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        // Get this week's hours
        Integer weekHours = worklogRepository.getTotalHoursByEmployee(employeeId, weekStart, weekEnd);
        if (weekHours == null) weekHours = 0;

        // Get today's hours
        List<Worklog> todayLogs = worklogRepository.findByEmployeeIdAndWorkDate(employeeId, LocalDate.now());
        int todayHours = todayLogs.stream().mapToInt(Worklog::getHoursWorked).sum();

        QuickStatsResponse.QuickStatsResponseBuilder statsBuilder = QuickStatsResponse.builder()
                .todayHours(todayHours)
                .weekHours(weekHours)
                .remainingWeekHours(Math.max(0, 40 - weekHours))
                .hasLoggedToday(!todayLogs.isEmpty());

        // Add role-specific stats
        if (employee.getRole() == Role.TEAM_LEAD) {
            // Team lead sees their direct team members
            List<Employee> teamMembers = employeeRepository.findByTeamLeadIdAndIsActiveTrue(employeeId);

            // Count how many have logged today
            int loggedToday = 0;
            for (Employee member : teamMembers) {
                boolean hasLogged = worklogRepository.hasLoggedWorkForDate(member.getId(), LocalDate.now());
                if (hasLogged) loggedToday++;
            }

            statsBuilder.teamSize(teamMembers.size());
            statsBuilder.teamMembersLoggedToday(loggedToday);
        } else if (employee.getRole() == Role.DIRECTOR) {
            // Director sees all department members (excluding self)
            List<Employee> departmentEmployees = employeeRepository.findByDepartmentIdAndIsActiveTrue(
                    employee.getDepartment().getId()
            );

            // Remove the director from the count
            int teamSize = departmentEmployees.size() - 1;
            statsBuilder.teamSize(teamSize);

            // Count how many have logged today (excluding director)
            int loggedToday = 0;
            for (Employee member : departmentEmployees) {
                if (!member.getId().equals(employeeId)) {
                    boolean hasLogged = worklogRepository.hasLoggedWorkForDate(member.getId(), LocalDate.now());
                    if (hasLogged) loggedToday++;
                }
            }

            statsBuilder.teamSize(teamSize);
            statsBuilder.teamMembersLoggedToday(loggedToday);
        }

        log.info("Quick stats for {} ({})", employee.getFullName(), employee.getRole());

        return statsBuilder.build();
    }

    // Helper methods
    private long calculateWeekends(LocalDate start, LocalDate end) {
        long weekends = 0;
        LocalDate date = start;
        while (!date.isAfter(end)) {
            if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
                weekends++;
            }
            date = date.plusDays(1);
        }
        return weekends;
    }

    private long calculateWorkingDays(LocalDate start, LocalDate end) {
        long totalDays = ChronoUnit.DAYS.between(start, end) + 1;
        return totalDays - calculateWeekends(start, end);
    }
}