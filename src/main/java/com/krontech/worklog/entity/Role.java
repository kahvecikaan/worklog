package com.krontech.worklog.entity;

import lombok.Getter;

@Getter
public enum Role {
    EMPLOYEE("Employee"),
    TEAM_LEAD("Team Lead"),
    DIRECTOR("Director");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

}