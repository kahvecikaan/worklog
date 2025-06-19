package com.krontech.worklog.service;

import com.krontech.worklog.dto.projection.DepartmentWithCountProjection;
import com.krontech.worklog.dto.projection.EmployeeHierarchyProjection;
import com.krontech.worklog.dto.response.*;
import com.krontech.worklog.entity.Department;
import com.krontech.worklog.entity.Employee;
import com.krontech.worklog.entity.Role;
import com.krontech.worklog.repository.DepartmentRepository;
import com.krontech.worklog.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;

    public List<DepartmentSummaryResponse> getAllDepartmentsWithStats() {
        List<DepartmentWithCountProjection> departmentsWithCounts =
                departmentRepository.findAllWithEmployeeCount();

        return departmentsWithCounts.stream()
                .map(dept -> DepartmentSummaryResponse.builder()
                        .id(dept.getId())
                        .name(dept.getName())
                        .code(dept.getCode())
                        .directorId(dept.getDirectorId())
                        .directorName(dept.getDirectorName())
                        .employeeCount(dept.getEmployeeCount())
                        .build())
                .collect(Collectors.toList());

    }

    public DepartmentHierarchyResponse getDepartmentHierarchy(Integer departmentId) {
        Department department = departmentRepository.findByIdWithDirector(departmentId)
                .orElseThrow(() -> new RuntimeException("Department not found"));

        List<EmployeeHierarchyProjection> employees = employeeRepository.findDepartmentHierarchy(departmentId);

        DepartmentHierarchyResponse.DepartmentHierarchyResponseBuilder responseBuilder =
                DepartmentHierarchyResponse.builder()
                        .department(department.getName())
                        .departmentCode(department.getCode());

        // Director
        EmployeeHierarchyProjection director = employees.stream()
                .filter(e -> e.getRole() == Role.DIRECTOR)
                .findFirst()
                .orElse(null);

        if (director != null) {
            responseBuilder.director(DepartmentHierarchyResponse.DirectorInfo.builder()
                    .id(director.getId())
                    .name(director.getFirstName() + " " + director.getLastName())
                    .email(director.getEmail())
                    .build());
        }

        // Team leads with their teams
        List<EmployeeHierarchyProjection> teamLeads = employees.stream()
                .filter(e -> e.getRole() == Role.TEAM_LEAD)
                .toList();

        List<DepartmentHierarchyResponse.TeamInfo> teams = new ArrayList<>();

        for(EmployeeHierarchyProjection lead : teamLeads) {
            // Get team members
            List<DepartmentHierarchyResponse.TeamMemberInfo> members = employees.stream()
                    .filter(e -> e.getTeamLead() != null && e.getTeamLead().getId().equals(lead.getId()))
                    .map(member -> DepartmentHierarchyResponse.TeamMemberInfo.builder()
                            .id(member.getId())
                            .name(member.getFirstName() + " " + member.getLastName())
                            .email(member.getEmail())
                            .grade(member.getGrade().getTitle())
                            .build())
                    .toList();

            teams.add(DepartmentHierarchyResponse.TeamInfo.builder()
                    .teamLeadId(lead.getId())
                    .teamLeadName(lead.getFirstName() + " " + lead.getLastName())
                    .teamLeadEmail(lead.getEmail())
                    .members(members)
                    .teamSize(members.size())
                    .build());
        }

        responseBuilder.teams(teams);
        responseBuilder.totalEmployees(employees.size());
        responseBuilder.totalTeamLeads(teamLeads.size());

        return responseBuilder.build();
    }

    public DepartmentDetailsResponse getDepartmentDetails(Integer departmentId) {
        Department department = departmentRepository.findByIdWithDirector(departmentId)
                .orElseThrow(() -> new RuntimeException("Department not found"));

        // Get employee count
        long employeeCount = employeeRepository.countByDepartmentIdAndIsActiveTrue(departmentId);

        // Get team lead count
        long teamLeadCount = employeeRepository.countByDepartmentIdAndRoleAndIsActiveTrue(
                departmentId, Role.TEAM_LEAD
        );

        return DepartmentDetailsResponse.builder()
                .id(departmentId)
                .name(department.getName())
                .code(department.getCode())
                .directorId(department.getDirector() != null ? department.getDirector().getId() : null)
                .directorName(department.getDirector() != null ? department.getDirector().getFullName() : null)
                .employeeCount(employeeCount)
                .teamLeadCount(teamLeadCount)
                .build();
    }

    public UserDepartmentResponse getUserDepartment(Integer userId) {
        Employee employee = employeeRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        Department department = employee.getDepartment();

        return UserDepartmentResponse.builder()
                .id(department.getId())
                .name(department.getName())
                .code(department.getCode())
                .directorName(department.getDirector() != null ?
                        department.getDirector().getFullName() : null)
                .build();
    }
}