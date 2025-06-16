package com.krontech.worklog.dto.projection;

public interface TeamMemberProjection {
    Integer getId();
    String getFirstName();
    String getLastName();
    Long getTotalHours();
    Long getDaysWorked();
}