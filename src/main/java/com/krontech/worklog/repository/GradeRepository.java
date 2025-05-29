package com.krontech.worklog.repository;

import com.krontech.worklog.entity.Grade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GradeRepository extends JpaRepository<Grade, Integer> {

    Optional<Grade> findByGradeLevel(Integer gradeLevel);
}