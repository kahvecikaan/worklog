package com.krontech.worklog.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private Integer id;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private Integer departmentId;
    private String departmentName;
}