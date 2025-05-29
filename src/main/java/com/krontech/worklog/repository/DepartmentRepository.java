package com.krontech.worklog.repository;

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

    // Get all departments with employee count
    @Query("""
        SELECT d, COUNT(e.id)
        FROM Department d
        LEFT JOIN d.employees e
        WHERE e.isActive = true OR e IS NULL
        GROUP BY d.id
        ORDER BY d.name
    """)
    List<Object[]> findAllWithEmployeeCount();
}