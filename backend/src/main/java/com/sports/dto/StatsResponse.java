package com.sports.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 统计数据响应DTO
 */
@Data
public class StatsResponse {
    private Integer totalExercises;
    private Integer totalDuration;
    private Integer totalCalories;
    private Integer activeGoals;
    private Integer completedGoals;
    // 目标达成汇总，按是否限定运动项目分组
    private List<Map<String, Object>> goalAchievement;
    private List<Map<String, Object>> caloriesTrend;
    private List<Map<String, Object>> exerciseTypeDistribution;
    private List<Map<String, Object>> weeklyStats;
}
