package com.sports.service;

import com.sports.dto.StatsResponse;
import com.sports.entity.Goal;
import com.sports.repository.ExerciseRepository;
import com.sports.repository.GoalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    
    @Transactional
    public StatsResponse getOverview(Long userId) {
        StatsResponse response = new StatsResponse();
        
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
        
        // 先按当前规则刷新所有目标状态，再统计，保证与目标页一致
        goalService.recalculateGoalsForUser(userId);
        int activeGoals = goalRepository.findByUserIdAndStatus(userId, "ACTIVE").size();
        int completedGoals = goalRepository.findByUserIdAndStatus(userId, "COMPLETED").size();
        response.setActiveGoals(activeGoals);
        response.setCompletedGoals(completedGoals);
        
        return response;
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

    @Transactional
    public Map<String, Object> getGoalAchievementSummary(Long userId) {
        goalService.recalculateGoalsForUser(userId);
        List<Goal> goals = goalRepository.findByUserIdOrderByCreatedAtDesc(userId);

        int totalCount = goals.size();
        int totalCompleted = 0;
        int specifiedCount = 0;
        int specifiedCompleted = 0;
        int unspecifiedCount = 0;
        int unspecifiedCompleted = 0;

        for (Goal goal : goals) {
            boolean completed = "COMPLETED".equals(goal.getStatus());
            if (completed) {
                totalCompleted++;
            }
            if (goal.getSportType() != null) {
                specifiedCount++;
                if (completed) {
                    specifiedCompleted++;
                }
            } else {
                unspecifiedCount++;
                if (completed) {
                    unspecifiedCompleted++;
                }
            }
        }

        Map<String, Object> specified = new HashMap<>();
        specified.put("totalGoals", specifiedCount);
        specified.put("completedGoals", specifiedCompleted);
        specified.put("achievementRate", calculateRate(specifiedCompleted, specifiedCount));

        Map<String, Object> unspecified = new HashMap<>();
        unspecified.put("totalGoals", unspecifiedCount);
        unspecified.put("completedGoals", unspecifiedCompleted);
        unspecified.put("achievementRate", calculateRate(unspecifiedCompleted, unspecifiedCount));

        Map<String, Object> result = new HashMap<>();
        result.put("specified", specified);
        result.put("unspecified", unspecified);
        result.put("totalGoals", totalCount);
        result.put("completedGoals", totalCompleted);
        result.put("achievementRate", calculateRate(totalCompleted, totalCount));
        return result;
    }

    private double calculateRate(int completed, int total) {
        if (total == 0) {
            return 0.0;
        }
        return Math.round((completed * 1000.0) / total) / 10.0;
    }
}
