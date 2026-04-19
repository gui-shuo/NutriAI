package com.nutriai.service;

import com.nutriai.entity.Merchant;
import com.nutriai.repository.MerchantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantService {

    private final MerchantRepository merchantRepository;

    public Page<Merchant> getMerchants(String keyword, String status, Pageable pageable) {
        if (keyword != null && !keyword.isBlank()) {
            return merchantRepository.searchByKeyword(keyword, pageable);
        }
        if (status != null && !status.isBlank()) {
            return merchantRepository.findByStatusOrderBySortOrderAsc(status, pageable);
        }
        return merchantRepository.findAllByOrderBySortOrderAsc(pageable);
    }

    public Merchant getMerchant(Long id) {
        return merchantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("商家不存在"));
    }

    /**
     * 根据ownerId获取商家（商家端登录后使用）
     */
    public Merchant getMerchantByOwnerId(Long ownerId) {
        return merchantRepository.findByOwnerId(ownerId)
                .orElseThrow(() -> new RuntimeException("未找到关联的店铺，请联系管理员"));
    }

    @Transactional
    public Merchant createMerchant(Merchant merchant) {
        Merchant saved = merchantRepository.save(merchant);
        log.info("创建商家: {} (id={})", saved.getName(), saved.getId());
        return saved;
    }

    @Transactional
    public Merchant updateMerchant(Long id, Merchant update) {
        Merchant existing = getMerchant(id);
        if (update.getName() != null) existing.setName(update.getName());
        if (update.getLogo() != null) existing.setLogo(update.getLogo());
        if (update.getPhone() != null) existing.setPhone(update.getPhone());
        if (update.getAddress() != null) existing.setAddress(update.getAddress());
        if (update.getLatitude() != null) existing.setLatitude(update.getLatitude());
        if (update.getLongitude() != null) existing.setLongitude(update.getLongitude());
        if (update.getDescription() != null) existing.setDescription(update.getDescription());
        if (update.getBusinessHours() != null) existing.setBusinessHours(update.getBusinessHours());
        if (update.getStatus() != null) existing.setStatus(update.getStatus());
        if (update.getType() != null) existing.setType(update.getType());
        if (update.getOwnerId() != null) existing.setOwnerId(update.getOwnerId());
        if (update.getSortOrder() != null) existing.setSortOrder(update.getSortOrder());
        Merchant saved = merchantRepository.save(existing);
        log.info("更新商家: {} (id={})", saved.getName(), id);
        return saved;
    }

    @Transactional
    public void deleteMerchant(Long id) {
        merchantRepository.deleteById(id);
        log.info("删除商家: id={}", id);
    }

    public Map<String, Object> getMerchantStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", merchantRepository.count());
        stats.put("active", merchantRepository.countByStatus("ACTIVE"));
        stats.put("inactive", merchantRepository.countByStatus("INACTIVE"));
        return stats;
    }

    // ==================== 公共接口方法 ====================

    /**
     * 获取附近门店列表，按Haversine公式计算距离并排序
     *
     * @param userLat  用户纬度
     * @param userLng  用户经度
     * @param limit    返回数量
     */
    public List<Map<String, Object>> getNearbyMerchants(Double userLat, Double userLng, int limit) {
        List<Merchant> activeMerchants = merchantRepository.findByStatusOrderBySortOrderAsc("ACTIVE");

        return activeMerchants.stream()
                .filter(m -> m.getLatitude() != null && m.getLongitude() != null)
                .map(m -> {
                    double distance = calculateDistance(userLat, userLng, m.getLatitude(), m.getLongitude());
                    Map<String, Object> map = merchantToMap(m);
                    map.put("distance", Math.round(distance)); // 距离(米)
                    map.put("distanceText", formatDistance(distance));
                    return map;
                })
                .sorted(Comparator.comparingDouble(m -> (double) ((Map<String, Object>) m).get("distance")))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有营业中的门店列表
     */
    public List<Map<String, Object>> getActiveMerchantList() {
        return merchantRepository.findByStatusOrderBySortOrderAsc("ACTIVE")
                .stream()
                .map(this::merchantToMap)
                .collect(Collectors.toList());
    }

    /**
     * 获取门店详情（可选带距离）
     */
    public Map<String, Object> getMerchantDetail(Long id, Double userLat, Double userLng) {
        Merchant merchant = getMerchant(id);
        Map<String, Object> map = merchantToMap(merchant);
        if (userLat != null && userLng != null
                && merchant.getLatitude() != null && merchant.getLongitude() != null) {
            double distance = calculateDistance(userLat, userLng, merchant.getLatitude(), merchant.getLongitude());
            map.put("distance", Math.round(distance));
            map.put("distanceText", formatDistance(distance));
        }
        return map;
    }

    /**
     * Haversine公式计算两点间的地球表面距离（米）
     */
    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        final double R = 6371000; // 地球平均半径(米)
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /**
     * 格式化距离显示文本
     */
    private String formatDistance(double distanceMeters) {
        if (distanceMeters < 1000) {
            return Math.round(distanceMeters) + "m";
        }
        return String.format("%.1fkm", distanceMeters / 1000.0);
    }

    /**
     * Merchant实体转Map（公共接口用，避免暴露ownerId等内部字段）
     */
    private Map<String, Object> merchantToMap(Merchant m) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", m.getId());
        map.put("name", m.getName());
        map.put("logo", m.getLogo());
        map.put("phone", m.getPhone());
        map.put("address", m.getAddress());
        map.put("latitude", m.getLatitude());
        map.put("longitude", m.getLongitude());
        map.put("description", m.getDescription());
        map.put("businessHours", m.getBusinessHours());
        map.put("type", m.getType());
        return map;
    }
}
