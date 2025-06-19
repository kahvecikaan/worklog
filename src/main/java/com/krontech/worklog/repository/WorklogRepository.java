package com.krontech.worklog.repository;

import com.krontech.worklog.dto.projection.*;
import com.krontech.worklog.entity.Worklog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface WorklogRepository extends JpaRepository<Worklog, Integer> {

    // Find worklogs by employee and date range
    List<Worklog> findByEmployeeIdAndWorkDateBetweenOrderByWorkDateDesc(
            Integer employeeId, LocalDate startDate, LocalDate endDate);

    // Find all worklogs for a specific date
    List<Worklog> findByEmployeeIdAndWorkDate(Integer employeeId, LocalDate workDate);

    // Dashboard query - Total hours by employee in date range
    @Query("""
    SELECT SUM(w.hoursWorked) FROM Worklog w
        WHERE w.employee.id = :employeeId
        AND w.workDate BETWEEN :startDate AND :endDate
    """)
    Integer getTotalHoursByEmployee(@Param("employeeId") Integer employeeId,
                                    @Param("startDate") LocalDate startDate,
                                    @Param("endDate") LocalDate endDate);

    // Dashboard query - Hours grouped by worklog type for an employee
    // Using interface projection instead of Object[]
    @Query("""
    SELECT w.worklogType.name as typeName, SUM(w.hoursWorked) as hours
    FROM Worklog w
    WHERE w.employee.id = :employeeId
        AND w.workDate BETWEEN :startDate AND :endDate
    GROUP BY w.worklogType.name
    ORDER BY SUM(w.hoursWorked) DESC
    """)
    List<WorklogTypeHoursProjection> getHoursByTypeForEmployee(@Param("employeeId") Integer employeeId,
                                                               @Param("startDate") LocalDate startDate,
                                                               @Param("endDate") LocalDate endDate);

    // Team Lead Dashboard - Get all worklogs for team members
    @Query("""
    SELECT w FROM Worklog w
    JOIN FETCH w.employee e
    JOIN FETCH w.worklogType
    WHERE e.teamLead.id = :teamLeadId
        AND w.workDate BETWEEN :startDate AND :endDate
    ORDER BY w.workDate DESC, e.firstName
    """)
    List<Worklog> findByTeamLeadId(@Param("teamLeadId") Integer teamLeadId,
                                   @Param("startDate") LocalDate startDate,
                                   @Param("endDate") LocalDate endDate);

    // Team Lead Dashboard - Summary by team member
    // Using interface projection
    @Query("""
    SELECT
        e.id as id,
        e.firstName as firstName,
        e.lastName as lastName,
        COALESCE(SUM(w.hoursWorked), 0) as totalHours,
        COUNT(DISTINCT w.workDate) as daysWorked
    FROM Employee e
    LEFT JOIN e.worklogs w ON w.workDate BETWEEN :startDate AND :endDate
    WHERE e.teamLead.id = :teamLeadId
        AND e.isActive = true
    GROUP BY e.id, e.firstName, e.lastName
    ORDER BY e.firstName
    """)
    List<TeamMemberProjection> getTeamSummary(@Param("teamLeadId") Integer teamLeadId,
                                              @Param("startDate") LocalDate startDate,
                                              @Param("endDate") LocalDate endDate);

    // Get all team members for a team lead
    @Query("""
    SELECT COUNT(e)
    FROM Employee e
    WHERE e.teamLead.id = :teamLeadId AND e.isActive = true
    """)
    Long getActiveTeamMemberCount(@Param("teamLeadId") Integer teamLeadId);

    // Director Dashboard - Department-wide worklogs
    @Query("""
    SELECT w FROM Worklog w
    JOIN FETCH w.employee e
    JOIN FETCH w.worklogType
    WHERE e.department.id = :deptId
        AND w.workDate BETWEEN :startDate AND :endDate
    ORDER BY w.workDate DESC
    """)
    List<Worklog> findByDepartmentId(@Param("deptId") Integer departmentId,
                                     @Param("startDate") LocalDate startDate,
                                     @Param("endDate") LocalDate endDate);

    // Director Dashboard - Summary by worklog type for entire department
    // Using interface projection
    @Query("""
    SELECT wt.name as typeName, SUM(w.hoursWorked) as hours
    FROM Worklog w
    JOIN w.worklogType wt
    JOIN w.employee e
    WHERE e.department.id = :deptId
        AND w.workDate BETWEEN :startDate AND :endDate
    GROUP BY wt.name
    ORDER BY SUM(w.hoursWorked) DESC
    """)
    List<WorklogTypeHoursProjection> getDepartmentWorklogTypeSummary(@Param("deptId") Integer departmentId,
                                                                     @Param("startDate") LocalDate startDate,
                                                                     @Param("endDate") LocalDate endDate);

    // Director Dashboard - Summary by team
    // Using DTO projection with constructor expression
    @Query("""
    SELECT new com.krontech.worklog.dto.projection.DepartmentTeamSummaryProjection(
        tl.id,
        tl.firstName,
        tl.lastName,
        COUNT(DISTINCT e.id),
        CAST(COALESCE(SUM(w.hoursWorked), 0) AS LONG)
    )
    FROM Employee e
    LEFT JOIN e.worklogs w ON w.workDate BETWEEN :startDate AND :endDate
    JOIN e.teamLead tl
    WHERE e.department.id = :deptId
    GROUP BY tl.id, tl.firstName, tl.lastName
    ORDER BY tl.firstName
    """)
    List<DepartmentTeamSummaryProjection> getDepartmentTeamSummary(@Param("deptId") Integer departmentId,
                                                                   @Param("startDate") LocalDate startDate,
                                                                   @Param("endDate") LocalDate endDate);

    // Get recent worklogs for an employee
    @Query("""
    SELECT w FROM Worklog w
    JOIN FETCH w.worklogType
    WHERE w.employee.id = :employeeId
    ORDER BY w.workDate DESC, w.createdAt DESC
    """)
    List<Worklog> findRecentByEmployeeId(@Param("employeeId") Integer employeeId);

    // Check if employee has logged work for a specific date
    @Query("SELECT COUNT(w) > 0 FROM Worklog w WHERE w.employee.id = :employeeId AND w.workDate = :date")
    boolean hasLoggedWorkForDate(@Param("employeeId") Integer employeeId, @Param("date") LocalDate date);

    @Query("""
    SELECT COUNT(w) FROM Worklog w
    JOIN w.employee e
    WHERE w.worklogType.id = :typeId
        AND w.workDate BETWEEN :startDate AND :endDate
        AND (:deptId IS NULL OR e.department.id = :deptId)
    """)
    Long countByWorklogTypeAndDateRange(@Param("typeId") Integer typeId,
                                        @Param("startDate") LocalDate startDate,
                                        @Param("endDate") LocalDate endDate,
                                        @Param("deptId") Integer departmentId);

    @Query("""
    SELECT w FROM Worklog w
    WHERE w.employee.id = :employeeId
        AND w.workDate = :workDate
        AND w.worklogType.id = :typeId
        AND (:projectName IS NULL AND w.projectName IS NULL OR w.projectName = :projectName)
        AND (:description IS NULL AND w.description IS NULL OR w.description = :description)
    """)
    Optional<Worklog> findDuplicate(
            @Param("employeeId") Integer employeeId,
            @Param("workDate") LocalDate workDate,
            @Param("typeId") Integer typeId,
            @Param("projectName") String projectName,
            @Param("description") String description
    );

    @Query("""
    SELECT w FROM Worklog w
    WHERE w.employee.id = :employeeId
        AND w.workDate = :workDate
    ORDER BY w.createdAt
    """)
    List<Worklog> findAllByEmployeeAndDate(
            @Param("employeeId") Integer employeeId,
            @Param("workDate") LocalDate workDate
    );
}