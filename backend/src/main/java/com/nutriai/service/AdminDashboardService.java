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
        
        DashboardStatsDTO.ChatStats chatStats = DashboardStatsDTO.ChatStats.builder()
                .totalChats(totalChats)
                .todayChats(todayChats)
                .yesterdayChats(yesterdayChats)
                .avgResponseTime(getAvgResponseTimeSafe(todayStart, now))
                .build();
        
        // AI统计（使用会话历史数据）
        long totalCalls = chatHistoryRepository.count();
        long todayCalls = chatHistoryRepository.countByCreatedAtAfter(todayStart);
        // 从日志表获取真实成功率
        long logTotal = chatLogRepository.countByCreatedAtBetween(todayStart, now);
        long logSuccess = chatLogRepository.countByStatusAndCreatedAtBetween("success", todayStart, now);
        double successRate = logTotal > 0 ? (logSuccess * 100.0 / logTotal) : 100.0;
        
        DashboardStatsDTO.AIStats aiStats = DashboardStatsDTO.AIStats.builder()
                .totalCalls(totalCalls)
                .todayCalls(todayCalls)
                .successRate(successRate)
                .avgTokens(getAvgTokensSafe(todayStart, now))
                .build();
        
        // 会员统计
        DashboardStatsDTO.MemberStats memberStats = DashboardStatsDTO.MemberStats.builder()
                .free(userRepository.countByMemberLevel("FREE"))
                .bronze(userRepository.countByMemberLevel("BRONZE"))
                .silver(userRepository.countByMemberLevel("SILVER"))
                .gold(userRepository.countByMemberLevel("GOLD"))
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
     * 获取AI使用趋势（从ai_chat_history表）
     */
    public List<TrendDataDTO> getAIUsageTrend(int days) {
        List<TrendDataDTO> trendData = new ArrayList<>();
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime startOfDay = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endOfDay = LocalDateTime.of(date, LocalTime.MAX);
            
            // 从ai_chat_history表统计
            long totalCalls = chatHistoryRepository.countByCreatedAtBetween(startOfDay, endOfDay);
            // 会话历史都是成功的
            long successCalls = totalCalls;
            
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
