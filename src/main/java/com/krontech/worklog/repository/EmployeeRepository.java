package com.krontech.worklog.repository;

import com.krontech.worklog.entity.Employee;
import com.krontech.worklog.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Integer> {

    // Basic queries
    Optional<Employee> findByEmail(String email);

    Optional<Employee> findByEmployeeCode(String employeeCode);

    boolean existsByEmail(String email);

    boolean existsByEmployeeCode(String employeeCode);

    // Find all active employees
    List<Employee> findByIsActiveTrue();

    // Find employees by role
    List<Employee> findByRole(Role role);

    // Find all employees under a specific team lead
    List<Employee> findByTeamLeadIdAndIsActiveTrue(Integer teamLeadId);

    // Find all employees in a department
    List<Employee> findByDepartmentIdAndIsActiveTrue(Integer departmentId);

    // Find all team leads in a department
    @Query("SELECT e FROM Employee e WHERE e.department.id = :deptId AND e.role = 'TEAM_LEAD' AND e.isActive = true")
    List<Employee> findTeamLeadsByDepartment(@Param("deptId") Long departmentId);

    // Get all employees visible to a specific user (for dropdowns in dashboard)
    @Query("""
        SELECT DISTINCT e FROM Employee e
        WHERE e.isActive = true AND (
            e.id = :userId OR
            (e.teamLead.id = :userId) OR
            (e.department.id = :deptId AND :userRole = 'DIRECTOR')
        )
        ORDER BY e.firstName, e.lastName
    """)
    List<Employee> findEmployeesVisibleToUser(@Param("userId") Integer userId,
                                              @Param("deptId") Integer departmentId,
                                              @Param("userRole") String role);

    // Get employee hierarchy for department
    @Query("""
    SELECT e FROM Employee e
    LEFT JOIN FETCH e.teamLead
    LEFT JOIN FETCH e.grade
    WHERE e.department.id = :deptId AND e.isActive = true
    ORDER BY
        CASE
            WHEN e.role = com.krontech.worklog.entity.Role.DIRECTOR THEN 1
            WHEN e.role = com.krontech.worklog.entity.Role.TEAM_LEAD THEN 2
            ELSE 3
        END,
        e.firstName
    """)
    List<Employee> findDepartmentHierarchy(@Param("deptId") Integer departmentId);

    Long countByDepartmentIdAndIsActiveTrue(Integer departmentId);

    Long countByDepartmentIdAndRoleAndIsActiveTrue(Integer departmentId, Role role);
}