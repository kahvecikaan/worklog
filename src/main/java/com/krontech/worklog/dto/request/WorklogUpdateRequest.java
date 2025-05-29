package com.krontech.worklog.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class WorklogUpdateRequest {
    @NotNull(message = "Worklog type is required")
    private Integer worklogTypeId;

    @NotNull(message = "Work date is required")
    @PastOrPresent(message = "Work date cannot be in the future")
    private LocalDate workDate;

    @NotNull(message = "Hours worked is required")
    @Min(value = 1, message = "Hours worked must be at least 1")
    @Max(value = 8, message = "Hours worked cannot exceed 8")
    private Integer hoursWorked;

    @NotBlank(message = "Description is required")
    @Size(min = 10, max = 500, message = "Description must be between 10 and 500 characters")
    private String description;

    @Size(max = 100, message = "Project name cannot exceed 100 characters")
    private String projectName;
}