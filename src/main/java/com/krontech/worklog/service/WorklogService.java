package com.krontech.worklog.service;

import com.krontech.worklog.dto.request.WorklogCreateRequest;
import com.krontech.worklog.dto.request.WorklogUpdateRequest;
import com.krontech.worklog.dto.response.WorklogResponse;
import com.krontech.worklog.entity.Employee;
import com.krontech.worklog.entity.Worklog;
import com.krontech.worklog.entity.WorklogType;
import com.krontech.worklog.repository.EmployeeRepository;
import com.krontech.worklog.repository.WorklogRepository;
import com.krontech.worklog.repository.WorklogTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorklogService {

    private final WorklogRepository worklogRepository;
    private final EmployeeRepository employeeRepository;
    private final WorklogTypeRepository worklogTypeRepository;

    @Transactional
    public WorklogResponse createWorklog(Integer employeeId, WorklogCreateRequest request) {
        // Check for duplicate entry
        worklogRepository.findByEmployeeIdAndWorkDateAndWorklogTypeId(
                employeeId, request.getWorkDate(), request.getWorklogTypeId()
        ).ifPresent(w -> {
            throw new RuntimeException("Worklog entry already exists for this date and type");
        });

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        WorklogType worklogType = worklogTypeRepository.findById(request.getWorklogTypeId())
                .orElseThrow(() -> new RuntimeException("Worklog type not found"));

        // Check if work date is not in the future
        if (request.getWorkDate().isAfter(LocalDate.now())) {
            throw new RuntimeException("Cannot log work for future dates");
        }

        // Check if employee was active on that date
        if (request.getWorkDate().isBefore(employee.getStartDate())) {
            throw new RuntimeException("Cannot log work before employment start date");
        }

        if (employee.getEndDate() != null && request.getWorkDate().isAfter(employee.getEndDate())) {
            throw new RuntimeException("Cannot log work after employment end date");
        }

        Worklog worklog = Worklog.builder()
                .employee(employee)
                .worklogType(worklogType)
                .workDate(request.getWorkDate())
                .hoursWorked(request.getHoursWorked())
                .description(request.getDescription())
                .projectName(request.getProjectName())
                .build();

        worklog = worklogRepository.save(worklog);
        return WorklogResponse.from(worklog);
    }

    @Transactional
    public WorklogResponse updateWorklog(Integer worklogId, Integer employeeId, WorklogUpdateRequest request) {
        Worklog worklog = worklogRepository.findById(worklogId)
                .orElseThrow(() -> new RuntimeException("Worklog not found"));

        // Verify ownership
        if (!worklog.getEmployee().getId().equals(employeeId)) {
            throw new RuntimeException("You can only edit your own worklogs");
        }

        // Check if editable (within 7 days)
        if (!worklog.isEditable()) {
            throw new RuntimeException("Worklog older than 7 days cannot be edited");
        }

        worklog.setHoursWorked(request.getHoursWorked());
        worklog.setDescription(request.getDescription());
        worklog.setProjectName(request.getProjectName());

        worklog = worklogRepository.save(worklog);
        return WorklogResponse.from(worklog);
    }

    @Transactional
    public void deleteWorklog(Integer worklogId, Integer employeeId) {
        Worklog worklog = worklogRepository.findById(worklogId)
                .orElseThrow(() -> new RuntimeException("Worklog not found"));

        // Verify ownership
        if (!worklog.getEmployee().getId().equals(employeeId)) {
            throw new RuntimeException("You can only delete your own worklogs");
        }

        // Check if deletable (within 7 days)
        if (!worklog.isEditable()) {
            throw new RuntimeException("Worklog older than 7 days cannot be deleted");
        }

        worklogRepository.delete(worklog);
    }

    @Transactional(readOnly = true)
    public WorklogResponse getWorklogById(Integer worklogId, Integer requesterId) {

        Worklog worklog = worklogRepository.findById(worklogId)
                .orElseThrow(() -> new RuntimeException("Worklog not found"));

        Employee requester = employeeRepository.findById(requesterId)
                .orElseThrow(() -> new RuntimeException("Requester not found"));

        // 1. Owner can always view
        if (worklog.getEmployee().getId().equals(requesterId)) {
            return WorklogResponse.from(worklog);
        }

        // 2. Team-lead can view direct reports
        if (requester.isTeamLead() && requester.canViewEmployee(worklog.getEmployee())) {
            return WorklogResponse.from(worklog);
        }

        // 3. Director can view anyone in their department
        if (requester.isDirector()
                && requester.getDepartment().equals(worklog.getEmployee().getDepartment())) {
            return WorklogResponse.from(worklog);
        }

        // Otherwise → forbidden
        throw new RuntimeException("You don’t have permission to view this worklog");
    }

    public List<WorklogResponse> getEmployeeWorklogs(Integer employeeId, LocalDate startDate, LocalDate endDate) {
        List<Worklog> worklogs = worklogRepository.findByEmployeeIdAndWorkDateBetweenOrderByWorkDateDesc(
                employeeId, startDate, endDate
        );

        return worklogs.stream()
                .map(WorklogResponse::from)
                .collect(Collectors.toList());
    }

    public List<WorklogResponse> getWorklogsForDate(Integer employeeId, LocalDate date) {
        List<Worklog> worklogs = worklogRepository.findByEmployeeIdAndWorkDate(employeeId, date);

        return worklogs.stream()
                .map(WorklogResponse::from)
                .collect(Collectors.toList());
    }

    // For team leads and directors to view team/department worklogs
    public List<WorklogResponse> getTeamWorklogs(Integer teamLeadId, LocalDate startDate, LocalDate endDate) {
        Employee teamLead = employeeRepository.findById(teamLeadId)
                .orElseThrow(() -> new RuntimeException("Team lead not found"));

        if (!teamLead.getRole().name().equals("TEAM_LEAD") && !teamLead.getRole().name().equals("DIRECTOR")) {
            throw new RuntimeException("Only team leads and directors can view team worklogs");
        }

        List<Worklog> worklogs = worklogRepository.findByTeamLeadId(teamLeadId, startDate, endDate);

        return worklogs.stream()
                .map(WorklogResponse::from)
                .collect(Collectors.toList());
    }

    public List<WorklogResponse> getTeamWorklogs(Integer teamLeadId, LocalDate startDate,
                                                 LocalDate endDate, Integer employeeId) {
        Employee teamLead = employeeRepository.findById(teamLeadId)
                .orElseThrow(() -> new RuntimeException("Team lead not found"));

        if (!teamLead.getRole().name().equals("TEAM_LEAD") && !teamLead.getRole().name().equals("DIRECTOR")) {
            throw new RuntimeException("Only team leads and directors can view team worklogs");
        }

        List<Worklog> worklogs;

        if (employeeId != null) {
            // Verify the employee is in the team
            Employee employee = employeeRepository.findById(employeeId)
                    .orElseThrow(() -> new RuntimeException("Employee not found"));

            if (!teamLead.canViewEmployee(employee)) {
                throw new RuntimeException("Employee is not in your team");
            }

            worklogs = worklogRepository.findByEmployeeIdAndWorkDateBetweenOrderByWorkDateDesc(
                    employeeId, startDate, endDate
            );
        } else {
            worklogs = worklogRepository.findByTeamLeadId(teamLeadId, startDate, endDate);
        }

        return worklogs.stream()
                .map(WorklogResponse::from)
                .collect(Collectors.toList());
    }

    public List<WorklogResponse> getDepartmentWorklogs(Integer directorId, LocalDate startDate,
                                                       LocalDate endDate, Integer teamLeadId, Integer employeeId) {
        Employee director = employeeRepository.findById(directorId)
                .orElseThrow(() -> new RuntimeException("Director not found"));

        if (!director.getRole().name().equals("DIRECTOR")) {
            throw new RuntimeException("Only directors can view department worklogs");
        }

        List<Worklog> worklogs;

        if (employeeId != null) {
            // Get specific employee's worklogs
            Employee employee = employeeRepository.findById(employeeId)
                    .orElseThrow(() -> new RuntimeException("Employee not found"));

            if (!employee.getDepartment().getId().equals(director.getDepartment().getId())) {
                throw new RuntimeException("Employee is not in your department");
            }

            worklogs = worklogRepository.findByEmployeeIdAndWorkDateBetweenOrderByWorkDateDesc(
                    employeeId, startDate, endDate
            );
        } else if (teamLeadId != null) {
            // Get specific team's worklogs
            worklogs = worklogRepository.findByTeamLeadId(teamLeadId, startDate, endDate);
        } else {
            // Get all department worklogs
            worklogs = worklogRepository.findByDepartmentId(
                    director.getDepartment().getId(), startDate, endDate
            );
        }

        return worklogs.stream()
                .map(WorklogResponse::from)
                .collect(Collectors.toList());
    }
}