package com.krontech.worklog.repository;

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

    // Check for duplicate entry
    Optional<Worklog> findByEmployeeIdAndWorkDateAndWorklogTypeId(
            Integer employeeId, LocalDate workDate, Integer worklogTypeId);

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
    @Query("""
        SELECT w.worklogType.name, SUM(w.hoursWorked)
        FROM Worklog w
        WHERE w.employee.id = :employeeId
        AND w.workDate BETWEEN :startDate AND :endDate
        GROUP BY w.worklogType.name
        ORDER BY SUM(w.hoursWorked) DESC
    """)
    List<Object[]> getHoursByTypeForEmployee(@Param("employeeId") Integer employeeId,
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
    @Query("""
        SELECT e.id, e.firstName, e.lastName, SUM(w.hoursWorked), COUNT(DISTINCT w.workDate)
        FROM Worklog w
        JOIN w.employee e
        WHERE e.teamLead.id = :teamLeadId
        AND w.workDate BETWEEN :startDate AND :endDate
        GROUP BY e.id, e.firstName, e.lastName
        ORDER BY e.firstName
    """)
    List<Object[]> getTeamSummary(@Param("teamLeadId") Integer teamLeadId,
                                  @Param("startDate") LocalDate startDate,
                                  @Param("endDate") LocalDate endDate);

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
    @Query("""
        SELECT wt.name, SUM(w.hoursWorked)
        FROM Worklog w
        JOIN w.worklogType wt
        JOIN w.employee e
        WHERE e.department.id = :deptId
        AND w.workDate BETWEEN :startDate AND :endDate
        GROUP BY wt.name
        ORDER BY SUM(w.hoursWorked) DESC
    """)
    List<Object[]> getDepartmentWorklogTypeSummary(@Param("deptId") Integer departmentId,
                                                   @Param("startDate") LocalDate startDate,
                                                   @Param("endDate") LocalDate endDate);

    // Director Dashboard - Summary by team
    @Query("""
        SELECT
            tl.id,
            tl.firstName,
            tl.lastName,
            COUNT(DISTINCT e.id) as teamSize,
            SUM(w.hoursWorked) as totalHours
        FROM Worklog w
        JOIN w.employee e
        JOIN e.teamLead tl
        WHERE e.department.id = :deptId
        AND w.workDate BETWEEN :startDate AND :endDate
        GROUP BY tl.id, tl.firstName, tl.lastName
        ORDER BY tl.firstName
    """)
    List<Object[]> getDepartmentTeamSummary(@Param("deptId") Integer departmentId,
                                            @Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate);

    // Get recent worklogs for an employee (for dashboard widget)
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

    @Query("SELECT CASE WHEN COUNT(w) > 0 THEN true ELSE false END FROM Worklog w WHERE w.employee.id = :employeeId AND w.workDate = :workDate")
    boolean existsByEmployeeIdAndWorkDate(@Param("employeeId") Integer employeeId,
                                          @Param("workDate") LocalDate workDate);
}