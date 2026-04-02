package com.nutriai.service;

import com.nutriai.dto.admin.DashboardStatsDTO;
import com.nutriai.dto.admin.TrendDataDTO;
import com.nutriai.repository.AIChatHistoryRepository;
import com.nutriai.repository.AIChatLogRepository;
import com.nutriai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 管理后台数据看板服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminDashboardService {
    
    private final UserRepository userRepository;
    private final AIChatLogRepository chatLogRepository;
    private final AIChatHistoryRepository chatHistoryRepository;
    
    /**
     * 获取数据看板统计数据
     */
    public DashboardStatsDTO getDashboardStats() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime yesterdayStart = todayStart.minusDays(1);
        LocalDateTime sevenDaysAgo = now.minusDays(7);
        
        // 用户统计
        DashboardStatsDTO.UserStats userStats = DashboardStatsDTO.UserStats.builder()
                .totalUsers(userRepository.count())
                .todayNewUsers(userRepository.countByCreatedAtAfter(todayStart))
                .yesterdayNewUsers(userRepository.countByCreatedAtBetween(yesterdayStart, todayStart))
                .activeUsers(userRepository.countByLastLoginTimeAfter(sevenDaysAgo))
                .build();
        
        // 对话统计（从ai_chat_history表）
        long totalChats = chatHistoryRepository.count();
        long todayChats = chatHistoryRepository.countByCreatedAtAfter(todayStart);
        long yesterdayChats = chatHistoryRepository.countByCreatedAtBetween(yesterdayStart, todayStart);
        
        // AI统计（ai_chat_log记录详细调用，ai_chat_history记录会话；两者取较大值避免空表问题）
        long logCalls = chatLogRepository.count();
        long historyCalls = chatHistoryRepository.count();
        long totalCalls = Math.max(logCalls, historyCalls);
        long todayLogCalls = chatLogRepository.countByCreatedAtBetween(todayStart, now);
        long todayHistoryCalls = chatHistoryRepository.countByCreatedAtBetween(todayStart, now);
        long todayCalls = Math.max(todayLogCalls, todayHistoryCalls);
        long logSuccess = chatLogRepository.countByStatus("success");
        double successRate = logCalls > 0 ? (logSuccess * 100.0 / logCalls) : 100.0;
        
        // 平均响应时间和Token：尝试全时间范围，无数据则用0
        LocalDateTime epochStart = LocalDateTime.of(LocalDate.of(2020, 1, 1), LocalTime.MIN);
        double avgTokens = getAvgTokensSafe(epochStart, now);
        double avgResponseTime = getAvgResponseTimeSafe(epochStart, now);
        
        DashboardStatsDTO.ChatStats chatStats = DashboardStatsDTO.ChatStats.builder()
                .totalChats(totalChats)
                .todayChats(todayChats)
                .yesterdayChats(yesterdayChats)
                .avgResponseTime(avgResponseTime)
                .build();
        
        DashboardStatsDTO.AIStats aiStats = DashboardStatsDTO.AIStats.builder()
                .totalCalls(totalCalls)
                .todayCalls(todayCalls)
                .successRate(successRate)
                .avgTokens(avgTokens)
                .build();
        
        // 会员统计
        long totalUsers = userRepository.count();
        long freeUsers = userRepository.countByMemberLevel("FREE");
        DashboardStatsDTO.MemberStats memberStats = DashboardStatsDTO.MemberStats.builder()
                .free(freeUsers)
                .vip(totalUsers - freeUsers)
                .build();
        
        return DashboardStatsDTO.builder()
                .userStats(userStats)
                .chatStats(chatStats)
                .aiStats(aiStats)
                .memberStats(memberStats)
                .build();
    }
    
    /**
     * 获取用户增长趋势
     */
    public List<TrendDataDTO> getUserGrowthTrend(int days) {
        List<TrendDataDTO> trendData = new ArrayList<>();
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime startOfDay = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endOfDay = LocalDateTime.of(date, LocalTime.MAX);
            
            long count = userRepository.countByCreatedAtBetween(startOfDay, endOfDay);
            
            trendData.add(TrendDataDTO.builder()
                    .date(date.format(formatter))
                    .count(count)
                    .build());
        }
        
        return trendData;
    }
    
    /**
     * 获取AI使用趋势（优先ai_chat_log，无数据时回退ai_chat_history）
     */
    public List<TrendDataDTO> getAIUsageTrend(int days) {
        List<TrendDataDTO> trendData = new ArrayList<>();
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime startOfDay = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endOfDay = LocalDateTime.of(date, LocalTime.MAX);
            
            long logCalls = chatLogRepository.countByCreatedAtBetween(startOfDay, endOfDay);
            long historyCalls = chatHistoryRepository.countByCreatedAtBetween(startOfDay, endOfDay);
            long totalCalls = Math.max(logCalls, historyCalls);
            long successCalls = logCalls > 0 
                    ? chatLogRepository.countByStatusAndCreatedAtBetween("success", startOfDay, endOfDay)
                    : totalCalls; // 无详细日志时视为全部成功
            
            trendData.add(TrendDataDTO.builder()
                    .date(date.format(formatter))
                    .count(totalCalls)
                    .success(successCalls)
                    .build());
        }
        
        return trendData;
    }
    
    /**
     * 安全获取平均响应时间，查询失败返回0
     */
    private double getAvgResponseTimeSafe(LocalDateTime start, LocalDateTime end) {
        try {
            Double avg = chatLogRepository.getAverageResponseTime(start, end);
            return avg != null ? avg : 0.0;
        } catch (Exception e) {
            log.warn("获取平均响应时间失败", e);
            return 0.0;
        }
    }
    
    /**
     * 安全获取平均Token使用量，查询失败返回0
     */
    private double getAvgTokensSafe(LocalDateTime start, LocalDateTime end) {
        try {
            Double avg = chatLogRepository.getAverageTokensUsed(start, end);
            return avg != null ? avg : 0.0;
        } catch (Exception e) {
            log.warn("获取平均Token使用量失败", e);
            return 0.0;
        }
    }
}
