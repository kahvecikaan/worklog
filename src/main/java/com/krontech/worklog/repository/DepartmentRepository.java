package com.krontech.worklog.repository;

import com.krontech.worklog.dto.projection.DepartmentWithCountProjection;
import com.krontech.worklog.dto.projection.DepartmentSummaryProjection;
import com.krontech.worklog.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Integer> {

    Optional<Department> findByCode(String code);

    boolean existsByCode(String code);

    // Find department with director info
    @Query("SELECT d FROM Department d LEFT JOIN FETCH d.director WHERE d.id = :id")
    Optional<Department> findByIdWithDirector(Integer id);

    // Get all departments with employee count using DTO projection
    @Query("""
    SELECT new com.krontech.worklog.dto.projection.DepartmentWithCountProjection(
        d.id,
        d.name,
        d.code,
        d.director.id,
        CONCAT(d.director.firstName, ' ', d.director.lastName),
        COUNT(e.id)
    )
    FROM Department d
    LEFT JOIN d.director dir
    LEFT JOIN d.employees e ON (e.isActive = true)
    GROUP BY d.id, d.name, d.code, d.director.id, d.director.firstName, d.director.lastName
    ORDER BY d.name
    """)
    List<DepartmentWithCountProjection> findAllWithEmployeeCount();

    @Query("""
    SELECT d.id as id,
           d.name as name,
           d.code as code,
           COUNT(e) as employeeCount
    FROM Department d
    LEFT JOIN d.employees e ON e.isActive = true
    GROUP BY d.id, d.name, d.code
    ORDER BY d.name
    """)
    List<DepartmentSummaryProjection> findAllDepartmentSummaries();
}