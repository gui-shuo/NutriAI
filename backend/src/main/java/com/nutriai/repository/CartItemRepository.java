package com.nutriai.repository;

import com.nutriai.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<CartItem> findByUserIdAndItemTypeAndItemId(Long userId, String itemType, Long itemId);

    @Query("SELECT COUNT(c) FROM CartItem c WHERE c.userId = :userId AND c.selected = true")
    int countSelectedByUserId(Long userId);

    @Modifying
    @Query("DELETE FROM CartItem c WHERE c.userId = :userId AND c.itemType = :itemType AND c.itemId = :itemId")
    void deleteByUserIdAndItemTypeAndItemId(Long userId, String itemType, Long itemId);

    @Modifying
    @Query("DELETE FROM CartItem c WHERE c.userId = :userId")
    void deleteAllByUserId(Long userId);
}
