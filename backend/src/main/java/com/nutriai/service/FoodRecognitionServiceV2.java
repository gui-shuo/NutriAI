package com.nutriai.service;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

/**
 * 食物识别服务 V2 - 图片识别使用天聚数行食物营养识别API
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

    @Value("${tianapi.api-key:}")
    private String tianapiKey;
    
    private static final String TIANAPI_URL = "https://apis.tianapi.com/imgnutrient/index";

    // ==================== 文本识别（不变） ====================

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
                    foods.add(buildFoodItemFromDB(nutrition, 0.95));
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

    // ==================== 图片识别（天聚数行API） ====================

    /**
     * 通过图片识别 - 使用天聚数行食物营养识别API
     */
    public FoodRecognitionResult recognizeByImage(Long userId, MultipartFile image) {
        log.info("收到图片识别请求，文件大小: {} bytes", image.getSize());

        if (tianapiKey == null || tianapiKey.isBlank()) {
            throw new UnsupportedOperationException("图片识别功能需要配置天聚数行API密钥，请联系管理员配置 TIANAPI_API_KEY");
        }

        long startTime = System.currentTimeMillis();

        try {
            byte[] imageBytes = image.getBytes();
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            // 异步上传COS
            String imageUrl = null;
            try {
                imageUrl = ossService.uploadFoodPhoto(image);
                log.info("识别图片已上传至COS: {}", imageUrl);
            } catch (Exception e) {
                log.warn("识别图片上传COS失败: {}", e.getMessage());
            }

            // 调用天聚数行API
            Map<String, Object> apiResult = callTianApi(base64Image);

            int code = (int) apiResult.get("code");
            if (code != 200) {
                String msg = (String) apiResult.getOrDefault("msg", "未知错误");
                log.warn("天聚数行API返回错误: code={}, msg={}", code, msg);
                if (code == 250) {
                    throw new IllegalStateException("未能识别到食物。请上传清晰的食物图片，本功能仅支持识别常见食物。");
                }
                throw new RuntimeException("食物识别服务异常（" + code + "）：" + msg);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> resultMap = (Map<String, Object>) apiResult.get("result");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> list = (List<Map<String, Object>>) resultMap.get("list");

            if (list == null || list.isEmpty()) {
                throw new IllegalStateException("未能识别到食物，请上传清晰的食物图片。");
            }

            List<FoodItem> foods = new ArrayList<>();
            for (Map<String, Object> item : list) {
                String type = getStringValue(item, "type");
                // 如果type是"未知"，跳过
                if ("未知".equals(type)) {
                    log.info("天聚数行返回未知类型，跳过");
                    continue;
                }
                foods.add(parseTianApiItem(item));
            }

            if (foods.isEmpty()) {
                throw new IllegalStateException("未能识别到食物。请上传清晰的食物图片，本功能仅支持识别常见食物。");
            }

            long endTime = System.currentTimeMillis();
            FoodRecognitionResult result = FoodRecognitionResult.builder()
                    .foods(foods)
                    .totalCount(foods.size())
                    .recognitionTime(endTime - startTime)
                    .imageUrl(imageUrl)
                    .build();

            saveRecognitionHistory(userId, "IMAGE", null, result);
            log.info("图片识别完成，识别到 {} 个食物，耗时: {}ms", foods.size(), result.getRecognitionTime());
            return result;

        } catch (IllegalStateException | UnsupportedOperationException e) {
            throw e;
        } catch (Exception e) {
            log.error("图片识别处理异常", e);
            throw new RuntimeException("图片识别失败，请稍后重试");
        }
    }

    /**
     * 调用天聚数行食物营养识别API
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> callTianApi(String base64Image) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("key", tianapiKey);
            params.add("img", base64Image);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    TIANAPI_URL, HttpMethod.POST, request, String.class);

            log.info("天聚数行API响应状态: {}", response.getStatusCode());
            return objectMapper.readValue(response.getBody(), Map.class);
        } catch (Exception e) {
            log.error("调用天聚数行API异常: {}", e.getMessage());
            throw new RuntimeException("食物识别服务调用失败: " + e.getMessage());
        }
    }

    /**
     * 解析天聚数行API单项结果为FoodItem
     */
    private FoodItem parseTianApiItem(Map<String, Object> item) {
        String name = getStringValue(item, "name");
        String type = getStringValue(item, "type");
        double trust = getDouble(item, "trust");

        FoodItem.NutritionInfo nutrition = FoodItem.NutritionInfo.builder()
                // 基础
                .energy(getDouble(item, "rl"))
                .protein(getDouble(item, "dbz"))
                .carbohydrate(getDouble(item, "shhf"))
                .fat(getDouble(item, "zf"))
                .fiber(getDouble(item, "ssxw"))
                // 矿物质
                .sodium(getDouble(item, "la"))
                .calcium(getDouble(item, "gai"))
                .potassium(getDouble(item, "jia"))
                .magnesium(getDouble(item, "mei"))
                .iron(getDouble(item, "tei"))
                .zinc(getDouble(item, "xin"))
                .phosphorus(getDouble(item, "ling"))
                .selenium(getDouble(item, "xi"))
                .copper(getDouble(item, "tong"))
                .manganese(getDouble(item, "meng"))
                // 维生素
                .vitaminA(getDouble(item, "wssa"))
                .vitaminC(getDouble(item, "wsfc"))
                .vitaminE(getDouble(item, "wsse"))
                .carotene(getDouble(item, "lb"))
                .thiamine(getDouble(item, "las"))
                .riboflavin(getDouble(item, "su"))
                .niacin(getDouble(item, "ys"))
                .retinolEquivalent(getDouble(item, "shc"))
                // 其他
                .cholesterol(getDouble(item, "dgc"))
                .source("tianapi")
                .build();

        return FoodItem.builder()
                .name(name)
                .confidence(trust)
                .category(type)
                .nutrition(nutrition)
                .build();
    }

    // ==================== 数据库查询 ====================

    private List<FoodNutrition> queryNutritionFromDatabase(String keyword) {
        try {
            List<FoodNutrition> ft = foodNutritionRepository.fullTextSearch(keyword);
            if (!ft.isEmpty()) return ft;
        } catch (Exception e) {
            log.warn("全文搜索异常（回退到LIKE）: keyword={}, error={}", keyword, e.getMessage());
        }
        var page = foodNutritionRepository.searchByKeyword(keyword, PageRequest.of(0, 5));
        return page.getContent();
    }

    /**
     * 从数据库实体构建FoodItem（文本识别用）
     */
    private FoodItem buildFoodItemFromDB(FoodNutrition n, double confidence) {
        return FoodItem.builder()
                .name(n.getFoodName())
                .confidence(confidence)
                .nutrition(FoodItem.NutritionInfo.builder()
                        .energy(n.getEnergy() != null ? n.getEnergy().doubleValue() : null)
                        .protein(n.getProtein() != null ? n.getProtein().doubleValue() : null)
                        .carbohydrate(n.getCarbohydrate() != null ? n.getCarbohydrate().doubleValue() : null)
                        .fat(n.getFat() != null ? n.getFat().doubleValue() : null)
                        .fiber(n.getDietaryFiber() != null ? n.getDietaryFiber().doubleValue() : null)
                        .sodium(n.getSodium() != null ? n.getSodium().doubleValue() : null)
                        .calcium(n.getCalcium() != null ? n.getCalcium().doubleValue() : null)
                        .potassium(n.getPotassium() != null ? n.getPotassium().doubleValue() : null)
                        .magnesium(n.getMagnesium() != null ? n.getMagnesium().doubleValue() : null)
                        .iron(n.getIron() != null ? n.getIron().doubleValue() : null)
                        .zinc(n.getZinc() != null ? n.getZinc().doubleValue() : null)
                        .phosphorus(n.getPhosphorus() != null ? n.getPhosphorus().doubleValue() : null)
                        .selenium(n.getSelenium() != null ? n.getSelenium().doubleValue() : null)
                        .vitaminA(n.getVitaminA() != null ? n.getVitaminA().doubleValue() : null)
                        .vitaminC(n.getVitaminC() != null ? n.getVitaminC().doubleValue() : null)
                        .vitaminE(n.getVitaminE() != null ? n.getVitaminE().doubleValue() : null)
                        .cholesterol(n.getCholesterol() != null ? n.getCholesterol().doubleValue() : null)
                        .source("database")
                        .build())
                .build();
    }

    // ==================== AI估算（文本识别兜底） ====================

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
            
            注意：只返回JSON，数值必须是数字，不要带单位
            """, foodName);
        
        try {
            String aiResponse = chatLanguageModel.generate(prompt);
            String jsonStr = extractJson(aiResponse);
            @SuppressWarnings("unchecked")
            var nutritionMap = objectMapper.readValue(jsonStr, Map.class);
            
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
                .confidence(0.75)
                .nutrition(nutrition)
                .build();
                
        } catch (Exception e) {
            log.error("AI估算失败，食物: {}，原因: {}", foodName, e.getMessage());
            double[] fb = getFallbackNutrition(foodName);
            return FoodItem.builder()
                .name(foodName)
                .confidence(0.3)
                .nutrition(FoodItem.NutritionInfo.builder()
                    .energy(fb[0]).protein(fb[1]).carbohydrate(fb[2]).fat(fb[3]).fiber(fb[4])
                    .source("default")
                    .build())
                .build();
        }
    }

    private double[] getFallbackNutrition(String name) {
        if (name == null) return new double[]{150, 8.0, 20.0, 5.0, 1.5};
        if (name.matches(".*[肉鸡鱼牛猪羊虾蛋鸭腿翅排].*")) return new double[]{180, 20.0, 2.0, 10.0, 0.0};
        if (name.matches(".*[菜瓜茄萝卜韭菠芹葱蒜椒笋].*")) return new double[]{25, 2.0, 4.0, 0.3, 2.0};
        if (name.matches(".*[豆腐浆].*")) return new double[]{80, 8.0, 4.0, 4.0, 1.5};
        if (name.matches(".*[果莓桃橙柚梨苹蕉葡芒柠].*")) return new double[]{60, 0.8, 15.0, 0.2, 1.5};
        if (name.matches(".*[饭面包糕粥米饺馒饼粉].*")) return new double[]{200, 5.0, 40.0, 1.5, 0.5};
        if (name.matches(".*[奶乳酪芝].*")) return new double[]{65, 3.5, 5.0, 3.0, 0.0};
        if (name.matches(".*[油坚果花生核桃腰果杏仁芝麻].*")) return new double[]{580, 15.0, 15.0, 55.0, 3.0};
        return new double[]{150, 8.0, 20.0, 5.0, 1.5};
    }

    // ==================== 工具方法 ====================

    private String extractJson(String response) {
        response = response.trim();
        if (response.startsWith("```json")) response = response.substring(7);
        if (response.startsWith("```")) response = response.substring(3);
        if (response.endsWith("```")) response = response.substring(0, response.length() - 3);
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        if (start >= 0 && end > start) return response.substring(start, end + 1);
        return response.trim();
    }

    private Double getDoubleValue(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try { return Double.parseDouble(value.toString()); }
        catch (Exception e) { return 0.0; }
    }

    private double getDouble(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val == null) return 0.0;
        if (val instanceof Number) return ((Number) val).doubleValue();
        try { return Double.parseDouble(val.toString()); }
        catch (Exception e) { return 0.0; }
    }

    private String getStringValue(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : "";
    }

    // ==================== 历史记录 ====================

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

    public List<FoodRecognitionHistory> getHistory(Long userId) {
        return recognitionHistoryRepository.findTop10ByUserIdOrderByCreatedAtDesc(userId);
    }

    public void deleteHistory(Long id, Long userId) {
        FoodRecognitionHistory history = recognitionHistoryRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("历史记录不存在"));
        if (!history.getUserId().equals(userId)) {
            throw new RuntimeException("无权删除此记录");
        }
        recognitionHistoryRepository.deleteById(id);
        log.info("用户 {} 删除了识别历史记录 {}", userId, id);
    }
}
