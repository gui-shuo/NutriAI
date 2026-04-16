package com.nutriai.service;

import com.nutriai.entity.*;
import com.nutriai.exception.BusinessException;
import com.nutriai.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
 * 购物车服务（支持 MEAL 和 PRODUCT 两类商品）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final MealItemRepository mealItemRepository;
    private final NutritionProductRepository productRepository;

    /** 获取用户购物车（含商品快照） */
    public List<Map<String, Object>> getCart(Long userId) {
        List<CartItem> items = cartItemRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<Map<String, Object>> result = new ArrayList<>();

        for (CartItem ci : items) {
            Map<String, Object> vo = new LinkedHashMap<>();
            vo.put("id", ci.getId());
            vo.put("itemType", ci.getItemType());
            vo.put("itemId", ci.getItemId());
            vo.put("quantity", ci.getQuantity());
            vo.put("selected", ci.getSelected());

            boolean invalid = false;
            if ("MEAL".equals(ci.getItemType())) {
                MealItem meal = mealItemRepository.findById(ci.getItemId()).orElse(null);
                if (meal == null || !meal.getIsAvailable()) {
                    invalid = true;
                    vo.put("name", meal != null ? meal.getName() : "已下架餐品");
                    vo.put("imageUrl", meal != null ? meal.getImageUrl() : null);
                    vo.put("price", BigDecimal.ZERO);
                    vo.put("stock", 0);
                } else {
                    vo.put("name", meal.getName());
                    vo.put("imageUrl", meal.getImageUrl());
                    vo.put("price", meal.getSalePrice());
                    vo.put("stock", meal.getStock());
                    vo.put("brief", meal.getBrief());
                }
            } else if ("PRODUCT".equals(ci.getItemType())) {
                NutritionProduct prod = productRepository.findById(ci.getItemId()).orElse(null);
                if (prod == null || "OFF_SALE".equals(prod.getStatus()) || "INACTIVE".equals(prod.getStatus())) {
                    invalid = true;
                    vo.put("name", prod != null ? prod.getName() : "已下架产品");
                    vo.put("imageUrl", prod != null ? prod.getImageUrl() : null);
                    vo.put("price", BigDecimal.ZERO);
                    vo.put("stock", 0);
                } else {
                    vo.put("name", prod.getName());
                    vo.put("imageUrl", prod.getImageUrl());
                    vo.put("price", prod.getSalePrice());
                    vo.put("stock", prod.getStock());
                    vo.put("brief", prod.getBrief());
                }
            }
            vo.put("invalid", invalid);
            if (invalid) vo.put("selected", false);
            result.add(vo);
        }
        return result;
    }

    /** 加入购物车 */
    @Transactional
    public CartItem addToCart(Long userId, String itemType, Long itemId, int quantity) {
        validateItem(itemType, itemId, quantity);

        Optional<CartItem> existing = cartItemRepository.findByUserIdAndItemTypeAndItemId(userId, itemType, itemId);
        if (existing.isPresent()) {
            CartItem ci = existing.get();
            ci.setQuantity(Math.min(ci.getQuantity() + quantity, 99));
            return cartItemRepository.save(ci);
        }

        return cartItemRepository.save(CartItem.builder()
                .userId(userId)
                .itemType(itemType)
                .itemId(itemId)
                .quantity(Math.min(quantity, 99))
                .selected(true)
                .build());
    }

    /** 更新数量 */
    @Transactional
    public CartItem updateQuantity(Long userId, Long cartItemId, int quantity) {
        CartItem ci = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new BusinessException("购物车条目不存在"));
        if (!ci.getUserId().equals(userId)) throw new BusinessException("无权操作");
        if (quantity <= 0) {
            cartItemRepository.delete(ci);
            return ci;
        }
        ci.setQuantity(Math.min(quantity, 99));
        return cartItemRepository.save(ci);
    }

    /** 更新选中状态 */
    @Transactional
    public void updateSelected(Long userId, Long cartItemId, boolean selected) {
        CartItem ci = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new BusinessException("购物车条目不存在"));
        if (!ci.getUserId().equals(userId)) throw new BusinessException("无权操作");
        ci.setSelected(selected);
        cartItemRepository.save(ci);
    }

    /** 全选/全不选 */
    @Transactional
    public void selectAll(Long userId, boolean selected) {
        List<CartItem> items = cartItemRepository.findByUserIdOrderByCreatedAtDesc(userId);
        items.forEach(ci -> ci.setSelected(selected));
        cartItemRepository.saveAll(items);
    }

    /** 删除条目 */
    @Transactional
    public void removeItem(Long userId, Long cartItemId) {
        CartItem ci = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new BusinessException("购物车条目不存在"));
        if (!ci.getUserId().equals(userId)) throw new BusinessException("无权操作");
        cartItemRepository.delete(ci);
    }

    /** 批量删除 */
    @Transactional
    public void removeItems(Long userId, List<Long> ids) {
        ids.forEach(id -> {
            cartItemRepository.findById(id).ifPresent(ci -> {
                if (ci.getUserId().equals(userId)) cartItemRepository.delete(ci);
            });
        });
    }

    /** 清空购物车 */
    @Transactional
    public void clearCart(Long userId) {
        cartItemRepository.deleteAllByUserId(userId);
    }

    /** 获取购物车已选商品数量 */
    public int getSelectedCount(Long userId) {
        return cartItemRepository.countSelectedByUserId(userId);
    }

    private void validateItem(String itemType, Long itemId, int quantity) {
        if (quantity <= 0) throw new BusinessException("数量必须大于0");
        if ("MEAL".equals(itemType)) {
            MealItem meal = mealItemRepository.findById(itemId)
                    .orElseThrow(() -> new BusinessException("餐品不存在"));
            if (!meal.getIsAvailable()) throw new BusinessException("餐品已下架");
        } else if ("PRODUCT".equals(itemType)) {
            NutritionProduct prod = productRepository.findById(itemId)
                    .orElseThrow(() -> new BusinessException("产品不存在"));
            if ("OFF_SALE".equals(prod.getStatus()) || "INACTIVE".equals(prod.getStatus()))
                throw new BusinessException("产品已下架");
        } else {
            throw new BusinessException("商品类型无效");
        }
    }
}
