package com.krontech.worklog.dto.response;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentDetailsResponse {
    private Integer id;
    private String name;
    private String code;
    private Integer directorId;
    private String directorName;
    private Long employeeCount;
    private Long teamLeadCount;
}
