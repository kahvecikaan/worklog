package com.krontech.worklog.service;

import com.krontech.worklog.dto.response.WorklogTypeResponse;
import com.krontech.worklog.entity.WorklogType;
import com.krontech.worklog.repository.WorklogTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorklogTypeService {

    private final WorklogTypeRepository worklogTypeRepository;

    public List<WorklogTypeResponse> getActiveWorklogTypes() {
        List<WorklogType> types = worklogTypeRepository.findByIsActiveTrue();

        return types.stream()
                .map(WorklogTypeResponse::from)
                .collect(Collectors.toList());
    }

    public WorklogTypeResponse getWorklogType(Integer id) {
        WorklogType type = worklogTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Worklog type not found"));

        return WorklogTypeResponse.from(type);
    }
}