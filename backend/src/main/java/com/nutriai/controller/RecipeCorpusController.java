package com.nutriai.controller;

import com.nutriai.entity.RecipeCorpus;
import com.nutriai.service.KnowledgeBaseService;
import com.nutriai.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 食谱语料库控制器
 * 提供155万+食谱的搜索和浏览API
 */
@RestController
@RequestMapping("/recipes/corpus")
@RequiredArgsConstructor
public class RecipeCorpusController {

    private final KnowledgeBaseService knowledgeBaseService;

    /**
     * 搜索/浏览食谱语料库
     */
    @GetMapping
    public ApiResponse<?> searchCorpus(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        if (size > 100) size = 100;
        Page<RecipeCorpus> result = knowledgeBaseService.searchCorpus(keyword, category, page, size);
        return ApiResponse.success("获取成功", result);
    }

    /**
     * 获取食谱语料库详情
     */
    @GetMapping("/{id}")
    public ApiResponse<?> getCorpusDetail(@PathVariable Long id) {
        return knowledgeBaseService.getCorpusDetail(id)
                .map(r -> ApiResponse.success("获取成功", r))
                .orElse(ApiResponse.error(404, "食谱不存在"));
    }

    /**
     * 获取分类列表及统计
     */
    @GetMapping("/categories")
    public ApiResponse<?> getCategories() {
        List<String> categories = knowledgeBaseService.getCorpusCategories();
        long total = knowledgeBaseService.getCorpusCount();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("categories", categories);
        data.put("total", total);
        return ApiResponse.success("获取成功", data);
    }
}
