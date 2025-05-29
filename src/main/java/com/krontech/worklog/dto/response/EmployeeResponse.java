package com.krontech.worklog.dto.response;

import com.krontech.worklog.entity.Employee;
import jakarta.persistence.criteria.CriteriaBuilder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeResponse {
    private Integer id;
    private String employeeCode;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private String grade;
    private String role;
    private Integer teamLeadId;
    private String teamLeadName;
    private Integer departmentId;
    private String departmentName;
    private Boolean isActive;

    public static EmployeeResponse from(Employee employee) {
        return EmployeeResponse.builder()
                .id(employee.getId())
                .employeeCode(employee.getEmployeeCode())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .fullName(employee.getFullName())
                .email(employee.getEmail())
                .grade(employee.getGrade().getTitle())
                .role(employee.getRole().getDisplayName())
                .teamLeadId(employee.getTeamLead() != null ? employee.getTeamLead().getId() : null)
                .teamLeadName(employee.getTeamLead() != null ? employee.getTeamLead().getFullName() : null)
                .departmentId(employee.getDepartment().getId())
                .departmentName(employee.getDepartment().getName())
                .isActive(employee.getIsActive())
                .build();
    }
}