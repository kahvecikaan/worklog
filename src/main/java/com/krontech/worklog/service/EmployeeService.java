package com.krontech.worklog.service;

import com.krontech.worklog.dto.request.LoginRequest;
import com.krontech.worklog.dto.response.EmployeeResponse;
import com.krontech.worklog.dto.response.LoginResponse;
import com.krontech.worklog.entity.Employee;
import com.krontech.worklog.entity.Role;
import com.krontech.worklog.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    public EmployeeResponse getEmployee(Integer id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        return EmployeeResponse.from(employee);
    }

    public List<EmployeeResponse> getEmployeesVisibleToUser(Integer userId) {
        Employee currentUser = employeeRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Employee> visibleEmployees = employeeRepository.findEmployeesVisibleToUser(
                userId,
                currentUser.getDepartment().getId(),
                currentUser.getRole().name()
        );

        return visibleEmployees.stream()
                .map(EmployeeResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getEmployeeIfAuthorized(Integer currentUserId, Integer targetEmployeeId) {
        Employee currentUser = findById(currentUserId);
        Employee targetEmployee = findById(targetEmployeeId);

        // Check if current user can view target employee
        if (!currentUser.canViewEmployee(targetEmployee)) {
            throw new RuntimeException("You don't have permission to view this employee");
        }

        return EmployeeResponse.from(targetEmployee);
    }

    @Transactional(readOnly = true)
    public List<EmployeeResponse> getDepartmentEmployees(Integer directorId) {
        Employee director = findById(directorId);

        if (!director.getRole().name().equals("DIRECTOR")) {
            throw new RuntimeException("Only directors can view all department employees");
        }

        List<Employee> employees = employeeRepository.findByDepartmentIdAndIsActiveTrue(
                director.getDepartment().getId()
        );

        return employees.stream()
                .map(EmployeeResponse::from)
                .collect(Collectors.toList());
    }

    public List<EmployeeResponse> getTeamMembers(Integer currentUserId, Integer teamLeadId) {
        Employee currentUser = findById(currentUserId);

        // Verify authorization
        if (currentUser.getRole() == Role.EMPLOYEE) {
            throw new RuntimeException("Only team leads and directors can view team members");
        }

        if (currentUser.getRole() == Role.TEAM_LEAD && !currentUser.getId().equals(teamLeadId)) {
            throw new RuntimeException("Team leads can only view their own team");
        }

        List<Employee> teamMembers = employeeRepository.findByTeamLeadIdAndIsActiveTrue(teamLeadId);

        return teamMembers.stream()
                .map(EmployeeResponse::from)
                .collect(Collectors.toList());
    }

    public Employee findById(Integer id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
    }
}