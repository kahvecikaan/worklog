package com.krontech.worklog.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuickStatsResponse {
    private Integer todayHours;
    private Integer weekHours;
    private Integer remainingWeekHours;
    private Boolean hasLoggedToday;

    // Role-specific fields (only populated for Team Lead and Director)
    private Integer teamSize;
    private Integer teamMembersLoggedToday;
}