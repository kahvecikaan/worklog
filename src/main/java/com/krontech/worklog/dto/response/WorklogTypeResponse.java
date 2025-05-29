package com.krontech.worklog.dto.response;

import com.krontech.worklog.entity.WorklogType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorklogTypeResponse {
    private Integer id;
    private String name;
    private String code;
    private Boolean isActive;

    public static WorklogTypeResponse from(WorklogType type) {
        return WorklogTypeResponse.builder()
                .id(type.getId())
                .name(type.getName())
                .code(type.getCode())
                .isActive(type.getIsActive())
                .build();
    }
}