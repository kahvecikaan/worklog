package com.krontech.worklog.dto.projection;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DepartmentTeamSummaryProjection {
    private Integer teamLeadId;
    private String teamLeadFirstName;
    private String teamLeadLastName;
    private Long teamSize;
    private Long totalHours;
}
