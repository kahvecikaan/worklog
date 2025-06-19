package com.krontech.worklog.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDepartmentResponse {
    private Integer id;
    private String name;
    private String code;
    private String directorName;
}
