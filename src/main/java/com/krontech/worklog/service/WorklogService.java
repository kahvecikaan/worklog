package com.krontech.worklog.service;

import com.krontech.worklog.dto.request.WorklogCreateRequest;
import com.krontech.worklog.dto.request.WorklogUpdateRequest;
import com.krontech.worklog.dto.response.WorklogResponse;
import com.krontech.worklog.entity.Employee;
import com.krontech.worklog.entity.Worklog;
import com.krontech.worklog.entity.WorklogType;
import com.krontech.worklog.exception.ValidationException;
import com.krontech.worklog.repository.EmployeeRepository;
import com.krontech.worklog.repository.WorklogRepository;
import com.krontech.worklog.repository.WorklogTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class WorklogService {

    private static final int MAX_DAILY_HOURS = 12; // Maximum hours per day
    private static final int STANDARD_DAILY_HOURS = 8; // Standard working hours
    private static final int MAX_ENTRIES_PER_TYPE = 3; // Maximum entries of same type per day

    private final WorklogRepository worklogRepository;
    private final EmployeeRepository employeeRepository;
    private final WorklogTypeRepository worklogTypeRepository;

    @Transactional
    public WorklogResponse createWorklog(Integer employeeId, WorklogCreateRequest request) {
        // First, validate the employee and basic constraints
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        WorklogType worklogType = worklogTypeRepository.findById(request.getWorklogTypeId())
                .orElseThrow(() -> new RuntimeException("Worklog type not found"));

        // Validate work date constraints
        validateWorkDate(request.getWorkDate(), employee);

        validateWorklogCreation(employeeId, request);

        // If all validations pass, create the worklog
        Worklog worklog = Worklog.builder()
                .employee(employee)
                .worklogType(worklogType)
                .workDate(request.getWorkDate())
                .hoursWorked(request.getHoursWorked())
                .description(request.getDescription())
                .projectName(request.getProjectName())
                .build();

        worklog = worklogRepository.save(worklog);
        log.info("Created worklog {} for employee {} on {}",
                worklog.getId(), employeeId, request.getWorkDate());

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

        // Update all fields including date and type
        if (request.getWorkDate() != null) {
            worklog.setWorkDate(request.getWorkDate());
        }

        if (request.getWorklogTypeId() != null) {
            WorklogType worklogType = worklogTypeRepository.findById(request.getWorklogTypeId())
                    .orElseThrow(() -> new RuntimeException("Invalid worklog type"));
            worklog.setWorklogType(worklogType);
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

    private void validateWorkDate(LocalDate workDate, Employee employee) {
        if (workDate.isAfter(LocalDate.now())) {
            throw new ValidationException("Cannot log work for future dates");
        }

        // Check if employee was active on that date
        if (workDate.isBefore(employee.getStartDate())) {
            throw new ValidationException("Cannot log work before employment start date");
        }

        if (employee.getEndDate() != null && workDate.isAfter(employee.getEndDate())) {
            throw new ValidationException("Cannot log work after employment end date");
        }
    }

    private void validateWorklogCreation(Integer employeeId, WorklogCreateRequest request) {
        // Step 1: Check for duplicates
        Optional<Worklog> exactDuplicate = worklogRepository.findDuplicate(
                employeeId,
                request.getWorkDate(),
                request.getWorklogTypeId(),
                request.getProjectName(),
                request.getDescription()
        );

        if (exactDuplicate.isPresent()) {
            throw new ValidationException(
                    "An identical worklog entry already exists for this date. " +
                            "Please update the existing entry or provide different details."
            );
        }

        // Step 2: Get all worklogs for the day to perform aggregate validations
        List<Worklog> dayWorklogs = worklogRepository.findAllByEmployeeAndDate(
                employeeId, request.getWorkDate()
        );

        // Step 3: Check total daily hours
        int currentDayTotal = dayWorklogs.stream()
                .mapToInt(Worklog::getHoursWorked)
                .sum();

        int newTotal = currentDayTotal + request.getHoursWorked();

        if (newTotal > MAX_DAILY_HOURS) {
            throw new ValidationException(String.format(
                    "Adding %d hours would exceed the maximum daily limit of %d hours. " +
                            "Current total: %d hours. Maximum additional hours: %d",
                    request.getHoursWorked(), MAX_DAILY_HOURS,
                    currentDayTotal, MAX_DAILY_HOURS - currentDayTotal
            ));
        }

        // Step 4: Warn if exceeding standard hours (but don't block)
        if (currentDayTotal >= STANDARD_DAILY_HOURS && newTotal > STANDARD_DAILY_HOURS) {
            log.warn("Employee {} logging {} hours for {}, exceeding standard {} hour day",
                    employeeId, newTotal, request.getWorkDate(), STANDARD_DAILY_HOURS);
        }

        // Step 5: Check for too many entries of the same type
        long sameTypeCount = dayWorklogs.stream()
                .filter(w -> w.getWorklogType().getId().equals(request.getWorklogTypeId()))
                .count();

        if (sameTypeCount >= MAX_ENTRIES_PER_TYPE) {
            throw new ValidationException(String.format(
                    "You already have %d entries for '%s' on this date. " +
                            "Consider updating an existing entry instead of creating a new one.",
                    sameTypeCount,
                    dayWorklogs.stream()
                            .filter(w -> w.getWorklogType().getId().equals(request.getWorklogTypeId()))
                            .findFirst()
                            .map(w -> w.getWorklogType().getName())
                            .orElse("this type")
            ));
        }
    }
}