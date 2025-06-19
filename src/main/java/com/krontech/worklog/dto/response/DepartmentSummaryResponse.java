package com.krontech.worklog.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentSummaryResponse {
    private Integer id;
    private String name;
    private String code;
    private Integer directorId;
    private String directorName;
    private Long employeeCount;
}
