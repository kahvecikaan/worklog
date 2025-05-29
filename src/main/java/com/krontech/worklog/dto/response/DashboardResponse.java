package com.krontech.worklog.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {
    private EmployeeSummary currentUser;
    private PeriodSummary periodSummary;
    private List<WorklogTypeBreakdown> worklogTypeBreakdown;
    private List<RecentWorklog> recentWorklogs;

    // For team leads and directors
    private List<TeamMemberSummary> teamMembers;
    private TeamStatistics teamStats;

    // For directors only
    private List<TeamLeadSummary> teamLeads;
    private DepartmentStatistics departmentStats;

    @Data
    @Builder
    public static class EmployeeSummary {
        private Integer id;
        private String name;
        private String role;
        private String department;
    }

    @Data
    @Builder
    public static class PeriodSummary {
        private Integer totalHours;
        private Double totalDays;
        private Integer daysWorked;
        private Double averageHoursPerDay;
        private String period; // "This Week", "This Month", etc.
    }

    @Data
    @Builder
    public static class WorklogTypeBreakdown {
        private String typeName;
        private Integer hours;
        private Double percentage;
    }

    @Data
    @Builder
    public static class RecentWorklog {
        private LocalDate date;
        private String type;
        private Integer hours;
        private String description;
        private String projectName;
    }

    @Data
    @Builder
    public static class TeamMemberSummary {
        private Integer id;
        private String name;
        private String grade;
        private Integer totalHours;
        private Integer daysWorked;
        private Double utilizationRate; // (totalHours / (workingDays * 8)) * 100
    }

    @Data
    @Builder
    public static class TeamStatistics {
        private Integer teamSize;
        private Integer totalTeamHours;
        private Double averageHoursPerMember;
        private Double teamUtilizationRate;
    }

    @Data
    @Builder
    public static class TeamLeadSummary {
        private Integer id;
        private String name;
        private Integer teamSize;
        private Integer teamTotalHours;
        private Double teamUtilizationRate;
    }

    @Data
    @Builder
    public static class DepartmentStatistics {
        private Integer totalEmployees;
        private Integer totalTeamLeads;
        private Integer departmentTotalHours;
        private Double departmentUtilizationRate;
    }
}