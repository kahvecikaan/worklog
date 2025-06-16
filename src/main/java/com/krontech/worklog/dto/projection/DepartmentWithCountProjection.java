package com.krontech.worklog.dto.projection;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DepartmentWithCountProjection {
    private Integer id;
    private String name;
    private String code;
    private Integer directorId;
    private String directorName;
    private Long employeeCount;
}