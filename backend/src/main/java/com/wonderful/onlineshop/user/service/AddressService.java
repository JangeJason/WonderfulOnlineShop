package com.wonderful.onlineshop.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wonderful.onlineshop.common.BusinessException;
import com.wonderful.onlineshop.user.entity.Address;
import com.wonderful.onlineshop.user.mapper.AddressMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AddressService {

    private final AddressMapper addressMapper;

    public AddressService(AddressMapper addressMapper) {
        this.addressMapper = addressMapper;
    }

    public List<Address> listByUserId(Long userId) {
        return addressMapper.selectList(
                new LambdaQueryWrapper<Address>()
                        .eq(Address::getUserId, userId)
                        .orderByDesc(Address::getIsDefault)
                        .orderByDesc(Address::getUpdatedAt));
    }

    @Transactional
    public Address addAddress(Long userId, Address req) {
        if (req.getIsDefault() != null && req.getIsDefault()) {
            addressMapper.clearDefaultAddress(userId);
        } else {
            // If this is the first address, make it default automatically
            Long count = addressMapper.selectCount(new LambdaQueryWrapper<Address>().eq(Address::getUserId, userId));
            if (count == 0) {
                req.setIsDefault(true);
            } else if (req.getIsDefault() == null) {
                req.setIsDefault(false);
            }
        }

        req.setUserId(userId);
        addressMapper.insert(req);
        return req;
    }

    @Transactional
    public void updateAddress(Long userId, Long addressId, Address req) {
        Address existing = addressMapper.selectById(addressId);
        if (existing == null || !existing.getUserId().equals(userId)) {
            throw new BusinessException("地址不存在或无权操作");
        }

        if (req.getIsDefault() != null && req.getIsDefault() && !existing.getIsDefault()) {
            addressMapper.clearDefaultAddress(userId);
        }

        req.setId(addressId);
        req.setUserId(userId);
        addressMapper.updateById(req);
    }

    public void deleteAddress(Long userId, Long addressId) {
        Address existing = addressMapper.selectById(addressId);
        if (existing == null || !existing.getUserId().equals(userId)) {
            throw new BusinessException("地址不存在或无权操作");
        }
        addressMapper.deleteById(addressId);
    }

    @Transactional
    public void setDefault(Long userId, Long addressId) {
        Address existing = addressMapper.selectById(addressId);
        if (existing == null || !existing.getUserId().equals(userId)) {
            throw new BusinessException("地址不存在或无权操作");
        }
        addressMapper.clearDefaultAddress(userId);
        existing.setIsDefault(true);
        addressMapper.updateById(existing);
    }
}
