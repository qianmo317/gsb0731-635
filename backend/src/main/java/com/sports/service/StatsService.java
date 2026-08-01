package com.sports.service;

import com.sports.dto.StatsResponse;
import com.sports.entity.Goal;
import com.sports.repository.ExerciseRepository;
import com.sports.repository.GoalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/**
 * 统计服务
 */
@Service
public class StatsService {
    
    @Autowired
    private ExerciseRepository exerciseRepository;
    
    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private GoalService goalService;

    public StatsResponse getOverview(Long userId) {
        StatsResponse response = new StatsResponse();

        // 先按现行规则刷新目标状态，保证统计与实际状态一致
        goalService.recalculateGoalsProgress(userId);

        // 本月统计
        LocalDate now = LocalDate.now();
        LocalDate monthStart = now.withDayOfMonth(1);
        LocalDate monthEnd = now.withDayOfMonth(now.lengthOfMonth());
        
        // 总运动次数
        Long totalCount = exerciseRepository.countByUserIdAndDateRange(userId, monthStart, monthEnd);
        response.setTotalExercises(totalCount != null ? totalCount.intValue() : 0);
        
        // 总时长
        Integer totalDuration = exerciseRepository.sumDurationByUserIdAndDateRange(userId, monthStart, monthEnd);
        response.setTotalDuration(totalDuration != null ? totalDuration : 0);
        
        // 总卡路里
        Integer totalCalories = exerciseRepository.sumCaloriesByUserIdAndDateRange(userId, monthStart, monthEnd);
        response.setTotalCalories(totalCalories != null ? totalCalories : 0);
        
        // 目标统计
        int activeGoals = goalRepository.findByUserIdAndStatus(userId, "ACTIVE").size();
        int completedGoals = goalRepository.findByUserIdAndStatus(userId, "COMPLETED").size();
        response.setActiveGoals(activeGoals);
        response.setCompletedGoals(completedGoals);
        
        return response;
    }
    
    /**
     * 目标达成汇总：按是否限定运动项目分组统计目标数、达成数和达成率
     */
    public Map<String, Object> getGoalAchievementSummary(Long userId) {
        // 先按现行规则刷新目标状态，保证汇总与实际状态一致
        goalService.recalculateGoalsProgress(userId);

        List<Goal> goals = goalRepository.findByUserIdOrderByCreatedAtDesc(userId);

        Map<String, Object> summary = new HashMap<>();
        summary.put("typed", summarize(goals.stream().filter(g -> g.getExerciseType() != null).toList()));
        summary.put("untyped", summarize(goals.stream().filter(g -> g.getExerciseType() == null).toList()));
        return summary;
    }

    private Map<String, Object> summarize(List<Goal> goals) {
        int total = goals.size();
        int achieved = (int) goals.stream().filter(g -> "COMPLETED".equals(g.getStatus())).count();
        double rate = total == 0 ? 0 : Math.round(achieved * 1000.0 / total) / 10.0;

        Map<String, Object> item = new HashMap<>();
        item.put("totalGoals", total);
        item.put("achievedGoals", achieved);
        item.put("achievementRate", rate);
        return item;
    }

    public Map<String, Object> getWeeklyStats(Long userId) {
        LocalDate now = LocalDate.now();
        LocalDate weekStart = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(6);
        
        Map<String, Object> stats = new HashMap<>();
        
        Long count = exerciseRepository.countByUserIdAndDateRange(userId, weekStart, weekEnd);
        Integer duration = exerciseRepository.sumDurationByUserIdAndDateRange(userId, weekStart, weekEnd);
        Integer calories = exerciseRepository.sumCaloriesByUserIdAndDateRange(userId, weekStart, weekEnd);
        
        stats.put("count", count != null ? count.intValue() : 0);
        stats.put("duration", duration != null ? duration : 0);
        stats.put("calories", calories != null ? calories : 0);
        stats.put("startDate", weekStart.toString());
        stats.put("endDate", weekEnd.toString());
        
        return stats;
    }
    
    public Map<String, Object> getMonthlyStats(Long userId) {
        LocalDate now = LocalDate.now();
        LocalDate monthStart = now.withDayOfMonth(1);
        LocalDate monthEnd = now.withDayOfMonth(now.lengthOfMonth());
        
        Map<String, Object> stats = new HashMap<>();
        
        Long count = exerciseRepository.countByUserIdAndDateRange(userId, monthStart, monthEnd);
        Integer duration = exerciseRepository.sumDurationByUserIdAndDateRange(userId, monthStart, monthEnd);
        Integer calories = exerciseRepository.sumCaloriesByUserIdAndDateRange(userId, monthStart, monthEnd);
        
        stats.put("count", count != null ? count.intValue() : 0);
        stats.put("duration", duration != null ? duration : 0);
        stats.put("calories", calories != null ? calories : 0);
        stats.put("month", now.getMonthValue());
        stats.put("year", now.getYear());
        
        return stats;
    }
    
    public List<Map<String, Object>> getCaloriesTrend(Long userId, int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);
        
        List<Object[]> data = exerciseRepository.getCaloriesTrendByDateRange(userId, startDate, endDate);
        
        // 创建日期到卡路里的映射
        Map<LocalDate, Integer> caloriesMap = new HashMap<>();
        for (Object[] row : data) {
            LocalDate date = (LocalDate) row[0];
            Long calories = (Long) row[1];
            caloriesMap.put(date, calories != null ? calories.intValue() : 0);
        }
        
        // 生成完整的日期序列
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDate date = startDate.plusDays(i);
            Map<String, Object> item = new HashMap<>();
            item.put("date", date.toString());
            item.put("calories", caloriesMap.getOrDefault(date, 0));
            result.add(item);
        }
        
        return result;
    }
    
    public List<Map<String, Object>> getExerciseTypeDistribution(Long userId) {
        List<Object[]> data = exerciseRepository.countByExerciseType(userId);
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : data) {
            Map<String, Object> item = new HashMap<>();
            item.put("type", row[0]);
            item.put("count", ((Long) row[1]).intValue());
            result.add(item);
        }
        
        return result;
    }
}
