package com.nutriai.service;

import com.nutriai.entity.UserAddress;
import com.nutriai.repository.UserAddressRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserAddressService {

    private final UserAddressRepository addressRepository;

    private static final int MAX_ADDRESS_COUNT = 20;

    public List<UserAddress> getUserAddresses(Long userId) {
        return addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId);
    }

    @Transactional
    public UserAddress addAddress(Long userId, UserAddress address) {
        long count = addressRepository.countByUserId(userId);
        if (count >= MAX_ADDRESS_COUNT) {
            throw new RuntimeException("最多只能保存" + MAX_ADDRESS_COUNT + "个收货地址");
        }

        address.setUserId(userId);

        // 如果是第一个地址或标记为默认，设为默认
        if (count == 0 || Boolean.TRUE.equals(address.getIsDefault())) {
            addressRepository.clearDefaultByUserId(userId);
            address.setIsDefault(true);
        }

        UserAddress saved = addressRepository.save(address);
        log.info("用户 {} 添加收货地址: {}", userId, saved.getId());
        return saved;
    }

    @Transactional
    public UserAddress updateAddress(Long userId, Long addressId, UserAddress update) {
        UserAddress existing = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new RuntimeException("地址不存在"));

        existing.setReceiverName(update.getReceiverName());
        existing.setReceiverPhone(update.getReceiverPhone());
        existing.setProvince(update.getProvince());
        existing.setCity(update.getCity());
        existing.setDistrict(update.getDistrict());
        existing.setDetailAddress(update.getDetailAddress());
        existing.setLabel(update.getLabel());

        if (Boolean.TRUE.equals(update.getIsDefault()) && !Boolean.TRUE.equals(existing.getIsDefault())) {
            addressRepository.clearDefaultByUserId(userId);
            existing.setIsDefault(true);
        }

        return addressRepository.save(existing);
    }

    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        UserAddress address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new RuntimeException("地址不存在"));

        addressRepository.delete(address);

        // 如果删除的是默认地址，将第一个地址设为默认
        if (Boolean.TRUE.equals(address.getIsDefault())) {
            List<UserAddress> remaining = addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId);
            if (!remaining.isEmpty()) {
                remaining.get(0).setIsDefault(true);
                addressRepository.save(remaining.get(0));
            }
        }

        log.info("用户 {} 删除收货地址: {}", userId, addressId);
    }

    @Transactional
    public UserAddress setDefault(Long userId, Long addressId) {
        UserAddress address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new RuntimeException("地址不存在"));

        addressRepository.clearDefaultByUserId(userId);
        address.setIsDefault(true);
        return addressRepository.save(address);
    }

    public UserAddress getDefaultAddress(Long userId) {
        return addressRepository.findByUserIdAndIsDefaultTrue(userId).orElse(null);
    }
}
