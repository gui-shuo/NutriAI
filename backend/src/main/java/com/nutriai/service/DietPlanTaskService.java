package com.nutriai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutriai.dto.DietPlanRequest;
import com.nutriai.dto.DietPlanResponse;
import com.nutriai.dto.TaskStatusResponse;
import com.nutriai.entity.DietPlanHistory;
import com.nutriai.entity.DietPlanTask;
import com.nutriai.repository.DietPlanHistoryRepository;
import com.nutriai.repository.DietPlanTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.Executor;

/**
 * 饮食计划任务服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DietPlanTaskService {
    
    private final DietPlanTaskRepository taskRepository;
    private final DietPlanHistoryRepository historyRepository;
    private final DietPlanService dietPlanService;
    private final ObjectMapper objectMapper;
    private final Executor asyncExecutor;
    
    /**
     * 创建生成任务
     */
    @Transactional
    public String createTask(Long userId, DietPlanRequest request) {
        String taskId = "task_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        
        DietPlanTask task = new DietPlanTask();
        task.setTaskId(taskId);
        task.setUserId(userId);
        task.setStatus("pending");
        task.setProgress(0);
        task.setTotalDays(request.getDays());
        task.setCurrentDay(0);
        
        try {
            task.setRequestData(objectMapper.writeValueAsString(request));
        } catch (Exception e) {
            log.error("序列化请求数据失败", e);
        }
        
        taskRepository.save(task);
        
        log.info("创建任务: taskId={}, userId={}, days={}", taskId, userId, request.getDays());
        log.info("准备异步执行任务...");
        
        // 使用Spring管理的线程池执行异步任务
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            log.info("异步线程开始执行: taskId={}", taskId);
            executeTaskAsync(taskId, userId, request);
        }, asyncExecutor);
        
        log.info("createTask方法返回，taskId={}", taskId);
        
        return taskId;
    }
    
    /**
     * 异步执行任务
     */
    @Async
    public void executeTaskAsync(String taskId, Long userId, DietPlanRequest request) {
        log.info("开始异步执行任务: taskId={}", taskId);
        
        try {
            // 检查任务是否已被取消
            if (isTaskCancelled(taskId)) {
                log.info("任务在开始前已被取消: taskId={}", taskId);
                return;
            }
            
            // 更新状态为运行中
            updateTaskStatus(taskId, "running", 0, 0, null, null);
            
            // 再次检查是否被取消
            if (isTaskCancelled(taskId)) {
                log.info("任务在执行中被取消: taskId={}", taskId);
                return;
            }
            
            // 调用原有的生成服务（传入taskId用于检查取消状态）
            log.info("准备调用生成服务，taskId={}", taskId);
            DietPlanResponse response = dietPlanService.generateDietPlanWithCancellation(userId, request, taskId, this);
            log.info("生成服务调用完成，taskId={}", taskId);
            
            // 最后检查是否被取消
            if (isTaskCancelled(taskId)) {
                log.info("任务在完成前被取消: taskId={}", taskId);
                return;
            }
            
            // 保存到历史记录
            saveToHistory(userId, response);
            
            // 更新任务状态为完成
            updateTaskStatus(taskId, "completed", 100, request.getDays(), response.getPlanId(), null);
            
            log.info("任务执行成功: taskId={}, planId={}", taskId, response.getPlanId());
            
        } catch (Exception e) {
            // 检查是否是因为取消导致的异常
            if (isTaskCancelled(taskId)) {
                log.info("任务已取消，忽略异常: taskId={}", taskId);
                return;
            }
            log.error("任务执行失败: taskId={}", taskId, e);
            updateTaskStatus(taskId, "failed", 0, 0, null, e.getMessage());
        }
    }
    
    /**
     * 检查任务是否已被取消
     */
    public boolean isTaskCancelled(String taskId) {
        return taskRepository.findByTaskId(taskId)
                .map(task -> "cancelled".equals(task.getStatus()))
                .orElse(false);
    }
    
    /**
     * 更新任务状态
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void updateTaskStatus(String taskId, String status, Integer progress, Integer currentDay, String planId, String errorMessage) {
        log.debug("更新任务状态: taskId={}, status={}, progress={}, currentDay={}", taskId, status, progress, currentDay);
        
        taskRepository.findByTaskId(taskId).ifPresent(task -> {
            task.setStatus(status);
            task.setProgress(progress);
            task.setCurrentDay(currentDay);
            if (planId != null) {
                task.setPlanId(planId);
            }
            if (errorMessage != null) {
                task.setErrorMessage(errorMessage);
            }
            DietPlanTask savedTask = taskRepository.save(task);
            log.debug("任务状态已保存: progress={}, currentDay={}", savedTask.getProgress(), savedTask.getCurrentDay());
        });
    }
    
    /**
     * 获取任务状态
     */
    public TaskStatusResponse getTaskStatus(String taskId) {
        return taskRepository.findByTaskId(taskId)
                .map(TaskStatusResponse::fromEntity)
                .orElse(null);
    }
    
    /**
     * 取消任务
     */
    @Transactional
    public boolean cancelTask(String taskId, Long userId) {
        log.info("尝试取消任务: taskId={}, userId={}", taskId, userId);
        
        return taskRepository.findByTaskId(taskId)
                .map(task -> {
                    log.info("找到任务: taskId={}, status={}, userId={}", taskId, task.getStatus(), task.getUserId());
                    
                    // 检查用户权限
                    if (!task.getUserId().equals(userId)) {
                        log.warn("用户ID不匹配: taskUserId={}, requestUserId={}", task.getUserId(), userId);
                        return false;
                    }
                    
                    // 检查任务状态
                    String currentStatus = task.getStatus();
                    if ("completed".equals(currentStatus)) {
                        log.warn("任务已完成，无法取消: taskId={}", taskId);
                        return false;
                    }
                    
                    if ("cancelled".equals(currentStatus)) {
                        log.info("任务已经是取消状态: taskId={}", taskId);
                        return true; // 已经取消了，返回true
                    }
                    
                    if ("failed".equals(currentStatus)) {
                        log.warn("任务已失败，无法取消: taskId={}", taskId);
                        return false;
                    }
                    
                    // pending 或 running 状态可以取消
                    task.setStatus("cancelled");
                    taskRepository.save(task);
                    log.info("任务已成功取消: taskId={}, 原状态={}", taskId, currentStatus);
                    return true;
                })
                .orElseGet(() -> {
                    log.error("未找到任务: taskId={}", taskId);
                    return false;
                });
    }
    
    /**
     * 保存到历史记录
     */
    @Transactional
    public void saveToHistory(Long userId, DietPlanResponse response) {
        DietPlanHistory history = new DietPlanHistory();
        history.setPlanId(response.getPlanId());
        history.setUserId(userId);
        history.setTitle(response.getTitle());
        history.setDays(response.getDays());
        history.setGoal(response.getGoalDescription());
        history.setMarkdownContent(response.getMarkdownContent());
        history.setIsFavorite(false);
        
        historyRepository.save(history);
        log.info("已保存到历史记录: planId={}", response.getPlanId());
    }
    
    /**
     * 创建修改任务（基于已有计划+用户修改建议）
     */
    @Transactional
    public String createModifyTask(Long userId, String originalPlanId, DietPlanResponse originalPlan, String suggestion) {
        String taskId = "task_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12) + "_mod";
        
        DietPlanTask task = new DietPlanTask();
        task.setTaskId(taskId);
        task.setUserId(userId);
        task.setStatus("pending");
        task.setProgress(0);
        task.setTotalDays(originalPlan.getDays());
        task.setCurrentDay(0);
        
        try {
            java.util.Map<String, String> reqData = new java.util.HashMap<>();
            reqData.put("originalPlanId", originalPlanId);
            reqData.put("suggestion", suggestion);
            task.setRequestData(objectMapper.writeValueAsString(reqData));
        } catch (Exception e) {
            log.error("序列化请求数据失败", e);
        }
        
        taskRepository.save(task);
        log.info("创建修改任务: taskId={}, userId={}, originalPlanId={}", taskId, userId, originalPlanId);
        
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            executeModifyTaskAsync(taskId, userId, originalPlan, suggestion);
        }, asyncExecutor);
        
        return taskId;
    }
    
    /**
     * 异步执行修改任务
     */
    public void executeModifyTaskAsync(String taskId, Long userId, DietPlanResponse originalPlan, String suggestion) {
        log.info("开始异步执行修改任务: taskId={}", taskId);
        
        try {
            if (isTaskCancelled(taskId)) {
                return;
            }
            
            updateTaskStatus(taskId, "running", 10, 0, null, null);
            
            DietPlanResponse modified = dietPlanService.modifyDietPlan(userId, originalPlan, suggestion, taskId, this);
            
            if (isTaskCancelled(taskId)) {
                return;
            }
            
            // 保存修改后的计划为新的历史记录
            saveToHistory(userId, modified);
            
            updateTaskStatus(taskId, "completed", 100, originalPlan.getDays(), modified.getPlanId(), null);
            log.info("修改任务执行成功: taskId={}, newPlanId={}", taskId, modified.getPlanId());
            
        } catch (Exception e) {
            if (isTaskCancelled(taskId)) {
                return;
            }
            log.error("修改任务执行失败: taskId={}", taskId, e);
            updateTaskStatus(taskId, "failed", 0, 0, null, e.getMessage());
        }
    }
}
