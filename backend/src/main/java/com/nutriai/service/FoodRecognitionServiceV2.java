package com.nutriai.service;

import com.baidu.aip.imageclassify.AipImageClassify;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutriai.dto.FoodItem;
import com.nutriai.dto.FoodRecognitionResult;
import com.nutriai.entity.FoodNutrition;
import com.nutriai.entity.FoodRecognitionHistory;
import com.nutriai.repository.FoodNutritionRepository;
import com.nutriai.repository.FoodRecognitionHistoryRepository;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 食物识别服务 V2 - 使用百度AI
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FoodRecognitionServiceV2 {
    
    private final FoodNutritionRepository foodNutritionRepository;
    private final FoodRecognitionHistoryRepository recognitionHistoryRepository;
    private final ChatLanguageModel chatLanguageModel;
    private final ObjectMapper objectMapper;
    private final OssService ossService;
    
    @Autowired(required = false)
    private AipImageClassify aipImageClassify;
    
    /**
     * 通过食物名称识别（文本输入）
     * 查找优先级：全文搜索 → LIKE模糊搜索 → AI大模型估算
     */
    public FoodRecognitionResult recognizeByName(Long userId, String foodName) {
        log.info("开始识别食物: {}", foodName);

        long startTime = System.currentTimeMillis();

        try {
            List<FoodNutrition> dbResults = queryNutritionFromDatabase(foodName);

            List<FoodItem> foods = new ArrayList<>();

            if (!dbResults.isEmpty()) {
                log.info("数据库命中 {} 条记录", dbResults.size());
                for (FoodNutrition nutrition : dbResults) {
                    foods.add(FoodItem.builder()
                            .name(nutrition.getFoodName())
                            .confidence(0.95)
                            .nutrition(FoodItem.NutritionInfo.builder()
                                    .energy(nutrition.getEnergy().doubleValue())
                                    .protein(nutrition.getProtein().doubleValue())
                                    .carbohydrate(nutrition.getCarbohydrate().doubleValue())
                                    .fat(nutrition.getFat().doubleValue())
                                    .fiber(nutrition.getDietaryFiber() != null ? nutrition.getDietaryFiber().doubleValue() : 0.0)
                                    .source("database")
                                    .build())
                            .build());
                }
            } else {
                log.info("数据库未命中，调用AI大模型估算: {}", foodName);
                foods.add(estimateNutritionByAI(foodName));
            }

            long endTime = System.currentTimeMillis();

            FoodRecognitionResult result = FoodRecognitionResult.builder()
                    .foods(foods)
                    .totalCount(foods.size())
                    .recognitionTime(endTime - startTime)
                    .imageUrl(null)
                    .build();

            saveRecognitionHistory(userId, "TEXT", foodName, result);

            log.info("食物识别完成，耗时: {}ms", result.getRecognitionTime());
            return result;

        } catch (Exception e) {
            log.error("食物名称识别失败: {}", foodName, e);
            throw new RuntimeException("食物识别失败，请稍后重试");
        }
    }

    /**
     * 数据库查询：先全文搜索，若无结果再 LIKE 模糊搜索（兼容短词/单字查询）
     */
    private List<FoodNutrition> queryNutritionFromDatabase(String keyword) {
        // 1. 全文搜索（适合较长中文词组，使用 MySQL FULLTEXT 或 ngram 分词）
        try {
            List<FoodNutrition> ft = foodNutritionRepository.fullTextSearch(keyword);
            if (!ft.isEmpty()) {
                return ft;
            }
        } catch (Exception e) {
            log.warn("全文搜索异常（将回退到LIKE搜索）: keyword={}, error={}", keyword, e.getMessage());
        }

        // 2. LIKE 模糊搜索回退（兼容单字、短词、不支持 FULLTEXT 的环境）
        var page = foodNutritionRepository.searchByKeyword(keyword, PageRequest.of(0, 5));
        return page.getContent();
    }
    
    /**
     * 通过图片识别 - 使用百度AI菜品识别API
     */
    public FoodRecognitionResult recognizeByImage(Long userId, MultipartFile image) {
        log.info("收到图片识别请求，文件大小: {} bytes", image.getSize());

        if (aipImageClassify == null) {
            throw new UnsupportedOperationException(
                    "图片识别功能需要配置百度AI，请联系管理员配置 BAIDU_APP_ID / BAIDU_API_KEY / BAIDU_SECRET_KEY");
        }

        long startTime = System.currentTimeMillis();

        try {
            byte[] imageBytes = image.getBytes();

            // COS上传和百度AI识别并行执行，节省10-20秒
            CompletableFuture<String> cosUploadFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    String url = ossService.uploadFoodPhoto(image);
                    log.info("识别图片已上传至COS: {}", url);
                    return url;
                } catch (Exception e) {
                    log.warn("识别图片上传COS失败，将不保存图片URL: {}", e.getMessage());
                    return null;
                }
            });

            // 调用百度AI菜品识别；filter_threshold 0.6 兼顾精度与召回
            HashMap<String, String> options = new HashMap<>();
            options.put("top_num", "5");
            options.put("filter_threshold", "0.6");
            options.put("baike_num", "0");

            JSONObject response = aipImageClassify.dishDetect(imageBytes, options);
            log.info("百度AI原始响应: {}", response.toString());

            // ① 检查百度平台级错误
            if (response.has("error_code")) {
                int errorCode = response.getInt("error_code");
                String errorMsg = response.optString("error_msg", "服务暂时不可用");
                log.error("百度AI识别接口返回错误: error_code={}, error_msg={}", errorCode, errorMsg);
                // 凭证类错误直接抛出，其他重试提示
                if (errorCode == 110 || errorCode == 111) {
                    throw new RuntimeException("百度AI授权异常（" + errorCode + "），请联系管理员");
                }
                throw new RuntimeException("百度AI识别服务出错（" + errorCode + "），请稍后重试");
            }

            // ② 解析识别结果
            List<FoodItem> foods = new ArrayList<>();

            if (response.has("result")) {
                JSONArray resultArray = response.getJSONArray("result");
                for (int i = 0; i < resultArray.length(); i++) {
                    JSONObject item = resultArray.getJSONObject(i);
                    String foodName = item.getString("name");
                    double probability = item.getDouble("probability");

                    // 百度API卡路里（唯一可信数据来源）
                    Double baiduCalorie = null;
                    if (item.has("calorie")) {
                        try {
                            baiduCalorie = Double.parseDouble(item.getString("calorie"));
                        } catch (NumberFormatException nfe) {
                            log.warn("解析卡路里失败: {}", item.optString("calorie"));
                        }
                    }

                    // 其他营养成分仅从数据库获取，不使用AI估算或虚假数据
                    FoodItem.NutritionInfo nutrition = getImageRecognitionNutrition(foodName, baiduCalorie);

                    foods.add(FoodItem.builder()
                            .name(foodName)
                            .confidence(probability)
                            .nutrition(nutrition)
                            .build());
                }
            }

            // ③ 无识别结果时给出明确业务提示
            if (foods.isEmpty()) {
                log.info("百度AI未从图片中识别到食物");
                throw new IllegalStateException("未能从图片中识别到食物，请确保图片清晰且包含可识别的食物");
            }

            long endTime = System.currentTimeMillis();

            // 等待COS上传完成并获取URL
            String uploadedImageUrl = cosUploadFuture.join();

            FoodRecognitionResult result = FoodRecognitionResult.builder()
                    .foods(foods)
                    .totalCount(foods.size())
                    .recognitionTime(endTime - startTime)
                    .imageUrl(uploadedImageUrl)
                    .build();

            saveRecognitionHistory(userId, "IMAGE", null, result);

            log.info("图片识别完成，识别到 {} 个食物，耗时: {}ms", foods.size(), result.getRecognitionTime());
            return result;

        } catch (IllegalStateException | UnsupportedOperationException e) {
            throw e; // 业务异常直接上抛
        } catch (Exception e) {
            log.error("图片识别处理异常", e);
            throw new RuntimeException("图片识别失败，请稍后重试");
        }
    }
    
    /**
     * 获取营养信息：优先从数据库查（全文→LIKE），否则 AI 估算
     */
    private FoodItem.NutritionInfo getNutritionInfo(String foodName) {
        List<FoodNutrition> dbResults = queryNutritionFromDatabase(foodName);

        if (!dbResults.isEmpty()) {
            FoodNutrition n = dbResults.get(0);
            return FoodItem.NutritionInfo.builder()
                    .energy(n.getEnergy().doubleValue())
                    .protein(n.getProtein().doubleValue())
                    .carbohydrate(n.getCarbohydrate().doubleValue())
                    .fat(n.getFat().doubleValue())
                    .fiber(n.getDietaryFiber() != null ? n.getDietaryFiber().doubleValue() : 0.0)
                    .source("database")
                    .build();
        }

        return estimateNutritionByAI(foodName).getNutrition();
    }

    /**
     * 图片识别专用：获取营养信息
     * 卡路里使用百度API返回值；其他营养成分仅从数据库查询，不使用AI估算或虚假数据
     * 
     * @param foodName 食物名称（百度识别结果）
     * @param baiduCalorie 百度API返回的卡路里（可能为null）
     */
    private FoodItem.NutritionInfo getImageRecognitionNutrition(String foodName, Double baiduCalorie) {
        List<FoodNutrition> dbResults = queryNutritionFromDatabase(foodName);

        if (!dbResults.isEmpty()) {
            FoodNutrition n = dbResults.get(0);
            // 数据库有完整数据，卡路里以百度为准（如果有的话）
            return FoodItem.NutritionInfo.builder()
                    .energy(baiduCalorie != null ? baiduCalorie : n.getEnergy().doubleValue())
                    .protein(n.getProtein().doubleValue())
                    .carbohydrate(n.getCarbohydrate().doubleValue())
                    .fat(n.getFat().doubleValue())
                    .fiber(n.getDietaryFiber() != null ? n.getDietaryFiber().doubleValue() : null)
                    .source("database")
                    .build();
        }

        // 数据库无数据：仅返回百度卡路里，其他营养成分缺失
        log.info("图片识别食物 '{}' 在数据库中未找到营养数据，仅返回百度卡路里", foodName);
        return FoodItem.NutritionInfo.builder()
                .energy(baiduCalorie)
                .protein(null)
                .carbohydrate(null)
                .fat(null)
                .fiber(null)
                .source("baidu-calorie-only")
                .build();
    }
    
    /**
     * 使用AI估算营养数据
     */
    private FoodItem estimateNutritionByAI(String foodName) {
        log.info("使用AI估算食物营养数据: {}", foodName);
        
        String prompt = String.format("""
            请估算以下食物的营养成分（每100g）：%s
            
            请严格按照以下JSON格式返回，不要添加任何其他文字：
            {
              "energy": 热量(kcal),
              "protein": 蛋白质(g),
              "carbohydrate": 碳水化合物(g),
              "fat": 脂肪(g),
              "fiber": 膳食纤维(g)
            }
            
            注意：
            1. 只返回JSON，不要有任何解释文字
            2. 数值必须是数字，不要带单位
            3. 如果不确定，给出合理的估算值
            """, foodName);
        
        try {
            String aiResponse = chatLanguageModel.generate(prompt);
            log.info("AI响应: {}", aiResponse);
            
            // 提取JSON部分
            String jsonStr = extractJson(aiResponse);
            
            // 解析JSON
            @SuppressWarnings("unchecked")
            var nutritionMap = objectMapper.readValue(jsonStr, java.util.Map.class);
            
            FoodItem.NutritionInfo nutrition = FoodItem.NutritionInfo.builder()
                .energy(getDoubleValue(nutritionMap.get("energy")))
                .protein(getDoubleValue(nutritionMap.get("protein")))
                .carbohydrate(getDoubleValue(nutritionMap.get("carbohydrate")))
                .fat(getDoubleValue(nutritionMap.get("fat")))
                .fiber(getDoubleValue(nutritionMap.get("fiber")))
                .source("estimated")
                .build();
            
            return FoodItem.builder()
                .name(foodName)
                .confidence(0.75) // AI估算置信度较低
                .nutrition(nutrition)
                .build();
                
        } catch (Exception e) {
            log.error("AI估算失败，食物: {}，原因: {}（请检查 AI_API_KEY 是否已配置）", foodName, e.getMessage());
            double[] fallback = getFallbackNutrition(foodName);
            return FoodItem.builder()
                .name(foodName)
                .confidence(0.3)
                .nutrition(FoodItem.NutritionInfo.builder()
                    .energy(fallback[0])
                    .protein(fallback[1])
                    .carbohydrate(fallback[2])
                    .fat(fallback[3])
                    .fiber(fallback[4])
                    .source("default")
                    .build())
                .build();
        }
    }

    /**
     * 根据食物名称关键词提供分类兜底营养数据（每100g）
     * 仅在数据库和AI均无法提供数据时使用
     */
    private double[] getFallbackNutrition(String name) {
        if (name == null) return new double[]{150, 8.0, 20.0, 5.0, 1.5};
        // 肉类/蛋类：高蛋白
        if (name.contains("肉") || name.contains("鸡") || name.contains("鱼") ||
                name.contains("牛") || name.contains("猪") || name.contains("羊") ||
                name.contains("虾") || name.contains("蛋") || name.contains("鸭") ||
                name.contains("腿") || name.contains("翅") || name.contains("排")) {
            return new double[]{180, 20.0, 2.0, 10.0, 0.0};
        }
        // 蔬菜类：低热低脂高纤
        if (name.contains("菜") || name.contains("瓜") || name.contains("茄") ||
                name.contains("萝卜") || name.contains("韭") || name.contains("菠") ||
                name.contains("芹") || name.contains("葱") || name.contains("蒜") ||
                name.contains("椒") || name.contains("笋") || name.contains("花椰")) {
            return new double[]{25, 2.0, 4.0, 0.3, 2.0};
        }
        // 豆制品：高蛋白高纤
        if (name.contains("豆") || name.contains("腐") || name.contains("浆")) {
            return new double[]{80, 8.0, 4.0, 4.0, 1.5};
        }
        // 水果类
        if (name.contains("果") || name.contains("莓") || name.contains("桃") ||
                name.contains("橙") || name.contains("柚") || name.contains("梨") ||
                name.contains("苹") || name.contains("香蕉") || name.contains("葡萄") ||
                name.contains("芒果") || name.contains("菠萝") || name.contains("柠檬")) {
            return new double[]{60, 0.8, 15.0, 0.2, 1.5};
        }
        // 主食/淀粉类：高碳水
        if (name.contains("饭") || name.contains("面") || name.contains("包") ||
                name.contains("糕") || name.contains("粥") || name.contains("米") ||
                name.contains("饺") || name.contains("馒") || name.contains("饼") ||
                name.contains("面条") || name.contains("粉")) {
            return new double[]{200, 5.0, 40.0, 1.5, 0.5};
        }
        // 乳制品
        if (name.contains("奶") || name.contains("乳") || name.contains("酸奶") ||
                name.contains("芝士") || name.contains("奶酪")) {
            return new double[]{65, 3.5, 5.0, 3.0, 0.0};
        }
        // 油脂/坚果类：高脂
        if (name.contains("油") || name.contains("坚果") || name.contains("花生") ||
                name.contains("核桃") || name.contains("腰果") || name.contains("杏仁") ||
                name.contains("芝麻")) {
            return new double[]{580, 15.0, 15.0, 55.0, 3.0};
        }
        // 通用兜底
        return new double[]{150, 8.0, 20.0, 5.0, 1.5};
    }
    
    /**
     * 从AI响应中提取JSON
     */
    private String extractJson(String response) {
        response = response.trim();
        if (response.startsWith("```json")) {
            response = response.substring(7);
        }
        if (response.startsWith("```")) {
            response = response.substring(3);
        }
        if (response.endsWith("```")) {
            response = response.substring(0, response.length() - 3);
        }
        
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        
        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }
        
        return response.trim();
    }
    
    /**
     * 转换为Double值
     */
    private Double getDoubleValue(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (Exception e) {
            return 0.0;
        }
    }
    
    /**
     * 保存识别历史
     */
    private void saveRecognitionHistory(Long userId, String recognitionType, String inputText, FoodRecognitionResult result) {
        try {
            String resultJson = objectMapper.writeValueAsString(result);
            
            FoodRecognitionHistory history = FoodRecognitionHistory.builder()
                .userId(userId)
                .recognitionType(recognitionType)
                .inputText(inputText)
                .imageUrl(result.getImageUrl())
                .recognitionResult(resultJson)
                .build();
            
            recognitionHistoryRepository.save(history);
            log.info("识别历史已保存: type={}, input={}", recognitionType, inputText);
            
        } catch (Exception e) {
            log.error("保存识别历史失败", e);
        }
    }
    
    /**
     * 获取识别历史
     */
    public List<FoodRecognitionHistory> getHistory(Long userId) {
        return recognitionHistoryRepository.findTop10ByUserIdOrderByCreatedAtDesc(userId);
    }
    
    /**
     * 删除识别历史
     */
    public void deleteHistory(Long id, Long userId) {
        // 验证记录是否属于当前用户
        FoodRecognitionHistory history = recognitionHistoryRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("历史记录不存在"));
        
        if (!history.getUserId().equals(userId)) {
            throw new RuntimeException("无权删除此记录");
        }
        
        recognitionHistoryRepository.deleteById(id);
        log.info("用户 {} 删除了识别历史记录 {}", userId, id);
    }
}
