package com.krontech.worklog.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentHierarchyResponse {
    private String department;
    private String departmentCode;
    private DirectorInfo director;
    private List<TeamInfo> teams;
    private Integer totalEmployees;
    private Integer totalTeamLeads;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DirectorInfo {
        private Integer id;
        private String name;
        private String email;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeamInfo {
        private Integer teamLeadId;
        private String teamLeadName;
        private String teamLeadEmail;
        private List<TeamMemberInfo> members;
        private Integer teamSize;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeamMemberInfo {
        private Integer id;
        private String name;
        private String email;
        private String grade;
    }
}
