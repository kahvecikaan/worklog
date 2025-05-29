package com.krontech.worklog.repository;

import com.krontech.worklog.entity.WorklogType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorklogTypeRepository extends JpaRepository<WorklogType, Integer> {
    List<WorklogType> findByIsActiveTrue();
    WorklogType findByCode(String code);
}