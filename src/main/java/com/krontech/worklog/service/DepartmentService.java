package com.krontech.worklog.service;

import com.krontech.worklog.entity.Department;
import com.krontech.worklog.entity.Employee;
import com.krontech.worklog.entity.Role;
import com.krontech.worklog.repository.DepartmentRepository;
import com.krontech.worklog.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;

    public List<Map<String, Object>> getAllDepartmentsWithStats() {
        List<Object[]> departmentsWithCounts = departmentRepository.findAllWithEmployeeCount();
        List<Map<String, Object>> result = new ArrayList<>();

        for (Object[] row : departmentsWithCounts) {
            Department dept = (Department) row[0];
            Long employeeCount = (Long) row[1];

            Map<String, Object> deptInfo = new HashMap<>();
            deptInfo.put("id", dept.getId());
            deptInfo.put("name", dept.getName());
            deptInfo.put("code", dept.getCode());
            deptInfo.put("directorId", dept.getDirector() != null ? dept.getDirector().getId() : null);
            deptInfo.put("directorName", dept.getDirector() != null ? dept.getDirector().getFullName() : null);
            deptInfo.put("employeeCount", employeeCount);

            result.add(deptInfo);
        }

        return result;
    }

    public Map<String, Object> getDepartmentHierarchy(Integer departmentId) {
        Department department = departmentRepository.findByIdWithDirector(departmentId)
                .orElseThrow(() -> new RuntimeException("Department not found"));

        List<Employee> employees = employeeRepository.findDepartmentHierarchy(departmentId);

        Map<String, Object> hierarchy = new HashMap<>();
        hierarchy.put("department", department.getName());
        hierarchy.put("departmentCode", department.getCode());

        // Director
        Employee director = employees.stream()
                .filter(e -> e.getRole().name().equals("DIRECTOR"))
                .findFirst()
                .orElse(null);

        if (director != null) {
            Map<String, Object> directorInfo = new HashMap<>();
            directorInfo.put("id", director.getId());
            directorInfo.put("name", director.getFullName());
            directorInfo.put("email", director.getEmail());
            hierarchy.put("director", directorInfo);
        }

        // Team Leads with their teams
        List<Map<String, Object>> teams = new ArrayList<>();
        List<Employee> teamLeads = employees.stream()
                .filter(e -> e.getRole().name().equals("TEAM_LEAD"))
                .toList();

        for (Employee teamLead : teamLeads) {
            Map<String, Object> teamInfo = new HashMap<>();
            teamInfo.put("teamLeadId", teamLead.getId());
            teamInfo.put("teamLeadName", teamLead.getFullName());
            teamInfo.put("teamLeadEmail", teamLead.getEmail());

            // Get team members
            List<Map<String, Object>> members = new ArrayList<>();
            employees.stream()
                    .filter(e -> e.getTeamLead() != null && e.getTeamLead().getId().equals(teamLead.getId()))
                    .forEach(member -> {
                        Map<String, Object> memberInfo = new HashMap<>();
                        memberInfo.put("id", member.getId());
                        memberInfo.put("name", member.getFullName());
                        memberInfo.put("email", member.getEmail());
                        memberInfo.put("grade", member.getGrade().getTitle());
                        members.add(memberInfo);
                    });

            teamInfo.put("members", members);
            teamInfo.put("teamSize", members.size());
            teams.add(teamInfo);
        }

        hierarchy.put("teams", teams);
        hierarchy.put("totalEmployees", employees.size() - 1); // Exclude director
        hierarchy.put("totalTeamLeads", teamLeads.size());

        return hierarchy;
    }

    public Map<String, Object> getDepartmentDetails(Integer departmentId, Integer currentUserId) {
        Employee currentUser = employeeRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Department department = departmentRepository.findByIdWithDirector(departmentId)
                .orElseThrow(() -> new RuntimeException("Department not found"));

        Map<String, Object> details = new HashMap<>();
        details.put("id", department.getId());
        details.put("name", department.getName());
        details.put("code", department.getCode());
        details.put("directorId", department.getDirector() != null ? department.getDirector().getId() : null);
        details.put("directorName", department.getDirector() != null ? department.getDirector().getFullName() : null);

        // Get employee count
        long employeeCount = employeeRepository.countByDepartmentIdAndIsActiveTrue(departmentId);
        details.put("employeeCount", employeeCount);

        // Get team lead count
        long teamLeadCount = employeeRepository.countByDepartmentIdAndRoleAndIsActiveTrue(
                departmentId, Role.TEAM_LEAD
        );
        details.put("teamLeadCount", teamLeadCount);

        return details;
    }

    public Map<String, Object> getUserDepartment(Integer userId) {
        Employee employee = employeeRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        Department department = employee.getDepartment();

        Map<String, Object> deptInfo = new HashMap<>();
        deptInfo.put("id", department.getId());
        deptInfo.put("name", department.getName());
        deptInfo.put("code", department.getCode());
        deptInfo.put("directorName", department.getDirector() != null ?
                department.getDirector().getFullName() : null);

        return deptInfo;
    }
}