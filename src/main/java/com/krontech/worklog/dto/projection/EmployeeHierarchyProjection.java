package com.krontech.worklog.dto.projection;

import com.krontech.worklog.entity.Role;

public interface EmployeeHierarchyProjection {
    Integer getId();
    String getFirstName();
    String getLastName();
    String getEmail();
    Role getRole();

    // Nested projection for grade
    GradeProjection getGrade();

    // Nested projection for team lead
    TeamLeadProjection getTeamLead();

    interface GradeProjection {
        String getTitle();
    }

    interface TeamLeadProjection {
        Integer getId();
        String getFirstName();
        String getLastName();
    }
}