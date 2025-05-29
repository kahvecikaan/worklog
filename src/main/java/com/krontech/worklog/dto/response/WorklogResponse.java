package com.krontech.worklog.dto.response;

import com.krontech.worklog.entity.Worklog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorklogResponse {
    private Integer id;
    private Integer employeeId;
    private String employeeName;
    private Integer worklogTypeId;
    private String worklogTypeName;
    private LocalDate workDate;
    private Integer hoursWorked;
    private Double daysWorked;
    private String description;
    private String projectName;
    private Boolean isEditable;

    public static WorklogResponse from(Worklog worklog) {
        return WorklogResponse.builder()
                .id(worklog.getId())
                .employeeId(worklog.getEmployee().getId())
                .employeeName(worklog.getEmployee().getFullName())
                .worklogTypeId(worklog.getWorklogType().getId())
                .worklogTypeName(worklog.getWorklogType().getName())
                .workDate(worklog.getWorkDate())
                .hoursWorked(worklog.getHoursWorked())
                .daysWorked(worklog.getWorkDays())
                .description(worklog.getDescription())
                .projectName(worklog.getProjectName())
                .isEditable(worklog.isEditable())
                .build();
    }
}