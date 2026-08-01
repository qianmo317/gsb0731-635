package com.sports.dto;

import lombok.Data;
import java.time.LocalDate;

/**
 * 目标响应DTO
 */
@Data
public class GoalResponse {
    private Long id;
    private String goalType;
    // 限定的运动类型信息；为 null 表示不限定（老目标统计口径不变）
    private Long exerciseTypeId;
    private String exerciseTypeName;
    private String exerciseTypeIcon;
    private Integer targetValue;
    private Integer currentValue;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private String title;
    private Integer progress;
}
