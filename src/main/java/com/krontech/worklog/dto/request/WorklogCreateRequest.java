package com.krontech.worklog.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;

@Data
public class WorklogCreateRequest {
    @NotNull(message = "Worklog type is required")
    private Integer worklogTypeId;

    @NotNull(message = "Work date is required")
    @PastOrPresent(message = "Cannot log work for future dates")
    private LocalDate workDate;

    @NotNull(message = "Hours worked is required")
    @Min(value = 1, message = "Minimum hours is 1")
    @Max(value = 8, message = "Maximum hours is 8")
    private Integer hoursWorked;

    private String description;

    @Size(max = 200, message = "Project name cannot exceed 200 characters")
    private String projectName;
}