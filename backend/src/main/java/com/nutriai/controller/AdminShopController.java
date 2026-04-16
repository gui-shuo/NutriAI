package com.nutriai.controller;

import com.nutriai.common.ApiResponse;
import com.nutriai.entity.MealItem;
import com.nutriai.entity.MealOrder;
import com.nutriai.entity.Nutritionist;
import com.nutriai.entity.NutritionProduct;
import com.nutriai.entity.ProductOrder;
import com.nutriai.repository.MealItemRepository;
import com.nutriai.repository.MealOrderRepository;
import com.nutriai.repository.NutritionistRepository;
import com.nutriai.repository.NutritionProductRepository;
import com.nutriai.repository.ProductOrderRepository;
import com.nutriai.service.MealService;
import com.nutriai.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理后台 - 营养师和产品管理
 */
@Slf4j
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminShopController {

    private final NutritionistRepository nutritionistRepository;
    private final NutritionProductRepository productRepository;
    private final MealItemRepository mealItemRepository;
    private final MealOrderRepository mealOrderRepository;
    private final ProductOrderRepository productOrderRepository;
    private final MealService mealService;
    private final ProductService productService;

    // ==================== 营养师管理 ====================

    @GetMapping("/nutritionists")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<Nutritionist>>> getNutritionists(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<Nutritionist> result;
        if (keyword != null && !keyword.isBlank()) {
            result = nutritionistRepository.searchByKeyword(keyword, PageRequest.of(page, size));
        } else {
            result = nutritionistRepository.findAllByOrderBySortOrderAsc(PageRequest.of(page, size));
        }
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/nutritionists/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Nutritionist>> getNutritionist(@PathVariable Long id) {
        Nutritionist n = nutritionistRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("营养师不存在"));
        return ResponseEntity.ok(ApiResponse.success(n));
    }

    @PostMapping("/nutritionists")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Nutritionist>> createNutritionist(@RequestBody Nutritionist nutritionist) {
        Nutritionist saved = nutritionistRepository.save(nutritionist);
        log.info("创建营养师: {}", saved.getName());
        return ResponseEntity.ok(ApiResponse.success("创建成功", saved));
    }

    @PutMapping("/nutritionists/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Nutritionist>> updateNutritionist(
            @PathVariable Long id, @RequestBody Nutritionist update) {
        Nutritionist existing = nutritionistRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("营养师不存在"));

        if (update.getName() != null) existing.setName(update.getName());
        if (update.getAvatar() != null) existing.setAvatar(update.getAvatar());
        if (update.getTitle() != null) existing.setTitle(update.getTitle());
        if (update.getSpecialties() != null) existing.setSpecialties(update.getSpecialties());
        if (update.getIntroduction() != null) existing.setIntroduction(update.getIntroduction());
        if (update.getExperienceYears() != null) existing.setExperienceYears(update.getExperienceYears());
        if (update.getConsultationFee() != null) existing.setConsultationFee(update.getConsultationFee());
        if (update.getStatus() != null) existing.setStatus(update.getStatus());
        if (update.getWorkingHours() != null) existing.setWorkingHours(update.getWorkingHours());
        if (update.getIsActive() != null) existing.setIsActive(update.getIsActive());
        if (update.getSortOrder() != null) existing.setSortOrder(update.getSortOrder());

        Nutritionist saved = nutritionistRepository.save(existing);
        log.info("更新营养师: {} (id={})", saved.getName(), id);
        return ResponseEntity.ok(ApiResponse.success("更新成功", saved));
    }

    @DeleteMapping("/nutritionists/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteNutritionist(@PathVariable Long id) {
        nutritionistRepository.deleteById(id);
        log.info("删除营养师: id={}", id);
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }

    @PutMapping("/nutritionists/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Nutritionist>> updateNutritionistStatus(
            @PathVariable Long id, @RequestBody Map<String, Object> body) {
        Nutritionist n = nutritionistRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("营养师不存在"));
        if (body.containsKey("status")) n.setStatus((String) body.get("status"));
        if (body.containsKey("isActive")) n.setIsActive((Boolean) body.get("isActive"));
        Nutritionist saved = nutritionistRepository.save(n);
        return ResponseEntity.ok(ApiResponse.success("状态更新成功", saved));
    }

    // ==================== 产品管理 ====================

    @GetMapping("/products")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<NutritionProduct>>> getProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<NutritionProduct> result;
        if (keyword != null && !keyword.isBlank()) {
            result = productRepository.adminSearchByKeyword(keyword, PageRequest.of(page, size));
        } else if (category != null && !category.isBlank()) {
            result = productRepository.findByCategoryOrderBySortOrderAsc(category, PageRequest.of(page, size));
        } else {
            result = productRepository.findAllByOrderBySortOrderAsc(PageRequest.of(page, size));
        }
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/products/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<NutritionProduct>> getProduct(@PathVariable Long id) {
        NutritionProduct p = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("产品不存在"));
        return ResponseEntity.ok(ApiResponse.success(p));
    }

    @PostMapping("/products")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<NutritionProduct>> createProduct(@RequestBody NutritionProduct product) {
        NutritionProduct saved = productRepository.save(product);
        log.info("创建产品: {}", saved.getName());
        return ResponseEntity.ok(ApiResponse.success("创建成功", saved));
    }

    @PutMapping("/products/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<NutritionProduct>> updateProduct(
            @PathVariable Long id, @RequestBody NutritionProduct update) {
        NutritionProduct existing = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("产品不存在"));

        if (update.getName() != null) existing.setName(update.getName());
        if (update.getImageUrl() != null) existing.setImageUrl(update.getImageUrl());
        if (update.getImageUrls() != null) existing.setImageUrls(update.getImageUrls());
        if (update.getCategory() != null) existing.setCategory(update.getCategory());
        if (update.getBrand() != null) existing.setBrand(update.getBrand());
        if (update.getBrief() != null) existing.setBrief(update.getBrief());
        if (update.getDescription() != null) existing.setDescription(update.getDescription());
        if (update.getOriginalPrice() != null) existing.setOriginalPrice(update.getOriginalPrice());
        if (update.getSalePrice() != null) existing.setSalePrice(update.getSalePrice());
        if (update.getStock() != null) existing.setStock(update.getStock());
        if (update.getTags() != null) existing.setTags(update.getTags());
        if (update.getSpecifications() != null) existing.setSpecifications(update.getSpecifications());
        if (update.getStatus() != null) existing.setStatus(update.getStatus());
        if (update.getIsRecommended() != null) existing.setIsRecommended(update.getIsRecommended());
        if (update.getSortOrder() != null) existing.setSortOrder(update.getSortOrder());

        NutritionProduct saved = productRepository.save(existing);
        log.info("更新产品: {} (id={})", saved.getName(), id);
        return ResponseEntity.ok(ApiResponse.success("更新成功", saved));
    }

    @DeleteMapping("/products/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteProduct(@PathVariable Long id) {
        productRepository.deleteById(id);
        log.info("删除产品: id={}", id);
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }

    @PutMapping("/products/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<NutritionProduct>> updateProductStatus(
            @PathVariable Long id, @RequestBody Map<String, String> body) {
        NutritionProduct p = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("产品不存在"));
        p.setStatus(body.get("status"));
        NutritionProduct saved = productRepository.save(p);
        return ResponseEntity.ok(ApiResponse.success("状态更新成功", saved));
    }

    // ==================== 营养餐管理 ====================

    @GetMapping("/meals")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<MealItem>>> getMeals(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<MealItem> result;
        if (keyword != null && !keyword.isBlank()) {
            result = mealItemRepository.adminSearchByKeyword(keyword, PageRequest.of(page, size));
        } else {
            result = mealItemRepository.findAllByOrderBySortOrderAsc(PageRequest.of(page, size));
        }
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/meals/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MealItem>> getMeal(@PathVariable Long id) {
        MealItem m = mealItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("餐品不存在"));
        return ResponseEntity.ok(ApiResponse.success(m));
    }

    @PostMapping("/meals")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MealItem>> createMeal(@RequestBody MealItem mealItem) {
        MealItem saved = mealItemRepository.save(mealItem);
        log.info("创建餐品: {}", saved.getName());
        return ResponseEntity.ok(ApiResponse.success("创建成功", saved));
    }

    @PutMapping("/meals/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MealItem>> updateMeal(
            @PathVariable Long id, @RequestBody MealItem update) {
        MealItem existing = mealItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("餐品不存在"));

        if (update.getName() != null) existing.setName(update.getName());
        if (update.getImageUrl() != null) existing.setImageUrl(update.getImageUrl());
        if (update.getImageUrls() != null) existing.setImageUrls(update.getImageUrls());
        if (update.getCategory() != null) existing.setCategory(update.getCategory());
        if (update.getMealType() != null) existing.setMealType(update.getMealType());
        if (update.getBrief() != null) existing.setBrief(update.getBrief());
        if (update.getDescription() != null) existing.setDescription(update.getDescription());
        if (update.getIngredients() != null) existing.setIngredients(update.getIngredients());
        if (update.getNutritionInfo() != null) existing.setNutritionInfo(update.getNutritionInfo());
        if (update.getOriginalPrice() != null) existing.setOriginalPrice(update.getOriginalPrice());
        if (update.getSalePrice() != null) existing.setSalePrice(update.getSalePrice());
        if (update.getStock() != null) existing.setStock(update.getStock());
        if (update.getDailyLimit() != null) existing.setDailyLimit(update.getDailyLimit());
        if (update.getTags() != null) existing.setTags(update.getTags());
        if (update.getIsRecommended() != null) existing.setIsRecommended(update.getIsRecommended());
        if (update.getIsAvailable() != null) existing.setIsAvailable(update.getIsAvailable());
        if (update.getAvailableDays() != null) existing.setAvailableDays(update.getAvailableDays());
        if (update.getAvailableStartTime() != null) existing.setAvailableStartTime(update.getAvailableStartTime());
        if (update.getAvailableEndTime() != null) existing.setAvailableEndTime(update.getAvailableEndTime());
        if (update.getSortOrder() != null) existing.setSortOrder(update.getSortOrder());

        MealItem saved = mealItemRepository.save(existing);
        log.info("更新餐品: {} (id={})", saved.getName(), id);
        return ResponseEntity.ok(ApiResponse.success("更新成功", saved));
    }

    @DeleteMapping("/meals/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteMeal(@PathVariable Long id) {
        mealItemRepository.deleteById(id);
        log.info("删除餐品: id={}", id);
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }

    @PutMapping("/meals/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MealItem>> updateMealStatus(
            @PathVariable Long id, @RequestBody Map<String, Object> body) {
        MealItem m = mealItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("餐品不存在"));
        if (body.containsKey("isAvailable")) m.setIsAvailable((Boolean) body.get("isAvailable"));
        MealItem saved = mealItemRepository.save(m);
        return ResponseEntity.ok(ApiResponse.success("状态更新成功", saved));
    }

    // ==================== 营养餐订单管理 ====================

    @GetMapping("/meal-orders")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<MealOrder>>> getMealOrders(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(mealService.getAllOrders(status, page, size)));
    }

    @PutMapping("/meal-orders/{orderNo}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MealOrder>> updateMealOrderStatus(
            @PathVariable String orderNo, @RequestBody Map<String, String> body) {
        String newStatus = body.get("status");
        if (newStatus == null || newStatus.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "缺少status参数"));
        }
        return ResponseEntity.ok(ApiResponse.success("状态更新成功", mealService.updateOrderStatus(orderNo, newStatus)));
    }

    @GetMapping("/meal-orders/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMealOrderStats() {
        return ResponseEntity.ok(ApiResponse.success(mealService.getOrderStats()));
    }

    @PostMapping("/meal-orders/verify-pickup")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MealOrder>> verifyPickupCode(@RequestBody Map<String, String> body) {
        String pickupCode = body.get("pickupCode");
        if (pickupCode == null || pickupCode.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "缺少取餐码"));
        }
        return ResponseEntity.ok(ApiResponse.success("核验成功", mealService.verifyPickupCode(pickupCode.trim())));
    }

    // ==================== 产品订单管理 ====================

    @GetMapping("/product-orders")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<ProductOrder>>> getProductOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<ProductOrder> result = productOrderRepository.adminSearch(
                (status != null && status.isBlank()) ? null : status,
                (keyword != null && keyword.isBlank()) ? null : keyword,
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/product-orders/{orderNo}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductOrder>> getProductOrder(@PathVariable String orderNo) {
        ProductOrder order = productOrderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new RuntimeException("订单不存在"));
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @PutMapping("/product-orders/{orderNo}/ship")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductOrder>> shipOrder(
            @PathVariable String orderNo, @RequestBody Map<String, String> body) {
        String company = body.get("shippingCompany");
        String trackingNo = body.get("trackingNo");
        return ResponseEntity.ok(ApiResponse.success("发货成功",
                productService.shipOrder(orderNo, company, trackingNo)));
    }

    @PutMapping("/product-orders/{orderNo}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductOrder>> updateProductOrderStatus(
            @PathVariable String orderNo, @RequestBody Map<String, String> body) {
        String newStatus = body.get("status");
        if (newStatus == null || newStatus.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "缺少status参数"));
        }
        return ResponseEntity.ok(ApiResponse.success("更新成功",
                productService.adminUpdateOrderStatus(orderNo, newStatus)));
    }

    @GetMapping("/product-orders/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProductOrderStats() {
        return ResponseEntity.ok(ApiResponse.success(productService.getOrderStats()));
    }
}
