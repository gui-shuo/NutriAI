package com.nutriai.controller;

import com.nutriai.entity.CartItem;
import com.nutriai.service.CartService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 购物车接口
 */
@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<?> getCart(HttpServletRequest request) {
        Long userId = getUserId(request);
        return ResponseEntity.ok(cartService.getCart(userId));
    }

    @GetMapping("/count")
    public ResponseEntity<?> getCount(HttpServletRequest request) {
        Long userId = getUserId(request);
        return ResponseEntity.ok(Map.of("count", cartService.getSelectedCount(userId)));
    }

    @PostMapping("/add")
    public ResponseEntity<?> add(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = getUserId(request);
        String itemType = (String) body.get("itemType");
        Long itemId = Long.parseLong(body.get("itemId").toString());
        int quantity = body.containsKey("quantity") ? Integer.parseInt(body.get("quantity").toString()) : 1;
        CartItem item = cartService.addToCart(userId, itemType, itemId, quantity);
        return ResponseEntity.ok(item);
    }

    @PutMapping("/{id}/quantity")
    public ResponseEntity<?> updateQuantity(@PathVariable Long id, @RequestBody Map<String, Integer> body,
                                            HttpServletRequest request) {
        Long userId = getUserId(request);
        int quantity = body.getOrDefault("quantity", 1);
        return ResponseEntity.ok(cartService.updateQuantity(userId, id, quantity));
    }

    @PutMapping("/{id}/select")
    public ResponseEntity<?> updateSelected(@PathVariable Long id, @RequestBody Map<String, Boolean> body,
                                            HttpServletRequest request) {
        Long userId = getUserId(request);
        cartService.updateSelected(userId, id, body.getOrDefault("selected", true));
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PutMapping("/select-all")
    public ResponseEntity<?> selectAll(@RequestBody Map<String, Boolean> body, HttpServletRequest request) {
        Long userId = getUserId(request);
        cartService.selectAll(userId, body.getOrDefault("selected", true));
        return ResponseEntity.ok(Map.of("success", true));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> remove(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getUserId(request);
        cartService.removeItem(userId, id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @DeleteMapping("/batch")
    public ResponseEntity<?> removeBatch(@RequestBody Map<String, List<Long>> body, HttpServletRequest request) {
        Long userId = getUserId(request);
        cartService.removeItems(userId, body.get("ids"));
        return ResponseEntity.ok(Map.of("success", true));
    }

    @DeleteMapping("/clear")
    public ResponseEntity<?> clear(HttpServletRequest request) {
        Long userId = getUserId(request);
        cartService.clearCart(userId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    private Long getUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }
}
