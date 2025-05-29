package com.krontech.worklog.dto.request;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

@Data
public class DashboardFilterRequest {
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    private Integer employeeId; // For team lead/director filtering
    private Integer teamLeadId; // For director filtering by team
    private String groupBy; // "employee", "worklogType", "date", "team"
}