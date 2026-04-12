package com.nutriai.service;

import com.nutriai.entity.FoodNutritionKb;
import com.nutriai.entity.RecipeCorpus;
import com.nutriai.repository.FoodNutritionKbRepository;
import com.nutriai.repository.RecipeCorpusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * 知识库服务
 * 统一管理食谱语料库(155万+)和食物营养数据(1354条)的查询
 * 为AI模块提供上下文参考数据
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseService {

    private final RecipeCorpusRepository recipeCorpusRepository;
    private final FoodNutritionKbRepository foodNutritionKbRepository;

    // Cache for expensive queries (refreshed every 30 min)
    private final AtomicLong cachedCorpusCount = new AtomicLong(-1);
    private final AtomicReference<List<String>> cachedCategories = new AtomicReference<>();
    private volatile long cacheTimestamp = 0;
    private static final long CACHE_TTL_MS = 30 * 60 * 1000;

    /**
     * 搜索食谱语料库（分页）
     */
    public Page<RecipeCorpus> searchCorpus(String keyword, String category, int page, int size) {
        int safePage = Math.max(page - 1, 0);
        Pageable pageable = PageRequest.of(safePage, size);
        if (keyword != null && !keyword.isBlank()) {
            return recipeCorpusRepository.searchPagedCapped(keyword.trim(), pageable);
        }
        if (category != null && !category.isBlank()) {
            return recipeCorpusRepository.findByCategoryCapped(category, pageable);
        }
        return recipeCorpusRepository.findAllCapped(pageable);
    }

    /**
     * 获取食谱语料库详情
     */
    public Optional<RecipeCorpus> getCorpusDetail(Long id) {
        return recipeCorpusRepository.findById(id);
    }

    /**
     * 获取所有分类（带缓存）
     */
    public List<String> getCorpusCategories() {
        refreshCacheIfNeeded();
        List<String> cached = cachedCategories.get();
        return cached != null ? cached : List.of();
    }

    /**
     * 获取语料库总量（带缓存）
     */
    public long getCorpusCount() {
        refreshCacheIfNeeded();
        return cachedCorpusCount.get();
    }

    private void refreshCacheIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - cacheTimestamp > CACHE_TTL_MS || cachedCorpusCount.get() < 0) {
            try {
                cachedCorpusCount.set(recipeCorpusRepository.countAll());
                cachedCategories.set(recipeCorpusRepository.findAllCategories());
                cacheTimestamp = now;
                log.info("知识库缓存已刷新: total={}, categories={}", cachedCorpusCount.get(), cachedCategories.get().size());
            } catch (Exception e) {
                log.warn("刷新知识库缓存失败: {}", e.getMessage());
            }
        }
    }

    /**
     * 搜索营养数据
     */
    public List<FoodNutritionKb> searchNutrition(String keyword, int limit) {
        if (keyword == null || keyword.isBlank()) return Collections.emptyList();
        try {
            List<FoodNutritionKb> results = foodNutritionKbRepository.fullTextSearch(keyword.trim(), limit);
            if (results.isEmpty()) {
                results = foodNutritionKbRepository.searchByName(keyword.trim(), limit);
            }
            return results;
        } catch (Exception e) {
            log.warn("营养数据搜索异常，降级为LIKE查询: {}", e.getMessage());
            return foodNutritionKbRepository.searchByName(keyword.trim(), limit);
        }
    }

    /**
     * 为AI模块提供食谱上下文
     * 根据关键词从语料库中搜索相关食谱，格式化为AI可读的文本
     */
    public String getRecipeContext(String keyword, int maxRecipes) {
        if (keyword == null || keyword.isBlank()) return "";
        
        try {
            List<RecipeCorpus> recipes = recipeCorpusRepository.searchByName(keyword.trim(), maxRecipes);
            if (recipes.isEmpty()) return "";

            StringBuilder sb = new StringBuilder();
            sb.append("\n### 相关食谱参考（来自食谱知识库）\n");
            for (int i = 0; i < recipes.size(); i++) {
                RecipeCorpus r = recipes.get(i);
                sb.append(String.format("%d. **%s**", i + 1, r.getName()));
                if (r.getDish() != null && !r.getDish().isEmpty()) {
                    sb.append("（").append(r.getDish()).append("）");
                }
                sb.append("\n");
                if (r.getDescription() != null && !r.getDescription().isEmpty()) {
                    String desc = r.getDescription().length() > 100
                            ? r.getDescription().substring(0, 100) + "..."
                            : r.getDescription();
                    sb.append("   ").append(desc).append("\n");
                }
                sb.append("   食材：").append(r.getIngredientsJson()).append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("获取食谱上下文失败: {}", e.getMessage());
            return "";
        }
    }

    /**
     * 为AI模块提供营养数据上下文
     * 搜索相关食物的营养成分信息
     */
    public String getNutritionContext(String keyword, int maxItems) {
        if (keyword == null || keyword.isBlank()) return "";

        List<FoodNutritionKb> items = searchNutrition(keyword, maxItems);
        if (items.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("\n### 相关食物营养数据参考\n");
        for (FoodNutritionKb item : items) {
            sb.append("- ").append(item.toNutritionSummary()).append("\n");
        }
        return sb.toString();
    }

    /**
     * 综合知识库查询 — 同时搜索食谱和营养数据
     * 用于AI对话中的上下文增强
     */
    public String getEnhancedContext(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) return "";

        // 提取关键词（取前20个字符作为搜索词）
        String keyword = extractKeyword(userMessage);
        if (keyword.isEmpty()) return "";

        StringBuilder context = new StringBuilder();

        // 食谱上下文（最多3条）
        String recipeCtx = getRecipeContext(keyword, 3);
        if (!recipeCtx.isEmpty()) {
            context.append(recipeCtx);
        }

        // 营养数据上下文（最多5条）
        String nutritionCtx = getNutritionContext(keyword, 5);
        if (!nutritionCtx.isEmpty()) {
            context.append(nutritionCtx);
        }

        return context.toString();
    }

    /**
     * 从用户消息中提取搜索关键词
     */
    private String extractKeyword(String message) {
        // 移除常见停用词和问句格式
        String cleaned = message
                .replaceAll("[？?！!。，,\\s]+", " ")
                .replaceAll("(请问|怎么|如何|什么|可以|帮我|推荐|做法|营养|告诉我|介绍一下)", "")
                .trim();
        
        // 取前面的实质性关键词
        String[] parts = cleaned.split("\\s+");
        StringBuilder kw = new StringBuilder();
        for (int i = 0; i < Math.min(parts.length, 3); i++) {
            if (!parts[i].isEmpty() && parts[i].length() >= 2) {
                if (kw.length() > 0) kw.append(" ");
                kw.append(parts[i]);
            }
        }
        return kw.toString();
    }
}
