package com.wonderful.onlineshop.aftersale.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wonderful.onlineshop.aftersale.entity.AfterSaleRequest;
import com.wonderful.onlineshop.aftersale.mapper.AfterSaleRequestMapper;
import com.wonderful.onlineshop.common.BusinessException;
import com.wonderful.onlineshop.order.dto.OrderDetailResponse;
import com.wonderful.onlineshop.order.entity.Order;
import com.wonderful.onlineshop.order.entity.OrderItem;
import com.wonderful.onlineshop.order.mapper.OrderItemMapper;
import com.wonderful.onlineshop.order.mapper.OrderMapper;
import com.wonderful.onlineshop.order.service.OrderService;
import com.wonderful.onlineshop.user.entity.User;
import com.wonderful.onlineshop.user.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AfterSaleService {
    private static final Set<String> VALID_REQUEST_TYPES = Set.of(
            "QUANTITY_SHORTAGE",
            "WRONG_PRODUCT",
            "PRINT_ISSUE",
            "PROCESS_ISSUE",
            "LOGISTICS_ISSUE",
            "RETURN_REFUND"
    );
    private static final Set<String> VALID_STATUS = Set.of("PENDING", "PROCESSING", "RESOLVED", "REJECTED", "CLOSED");

    private final AfterSaleRequestMapper afterSaleRequestMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderService orderService;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AfterSaleService(AfterSaleRequestMapper afterSaleRequestMapper,
                            OrderMapper orderMapper,
                            OrderItemMapper orderItemMapper,
                            OrderService orderService,
                            UserMapper userMapper) {
        this.afterSaleRequestMapper = afterSaleRequestMapper;
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.orderService = orderService;
        this.userMapper = userMapper;
    }

    @Transactional
    public AfterSaleDTO create(Long userId, CreateAfterSaleRequest req) {
        if (req.itemIds() == null || req.itemIds().isEmpty()) {
            throw new BusinessException("请至少选择一个售后商品");
        }
        if (!VALID_REQUEST_TYPES.contains(req.requestType())) {
            throw new BusinessException("售后类型无效");
        }
        Order order = orderMapper.selectById(req.orderId());
        if (order == null || !Objects.equals(order.getUserId(), userId)) {
            throw new BusinessException("订单不存在");
        }
        boolean canApply = "COMPLETED".equals(order.getStatus()) ||
                ("SHIPPED".equals(order.getStatus()) && "SIGNED".equals(order.getShippingStatus()));
        if (!canApply) {
            throw new BusinessException("仅已签收或已完成订单可申请售后");
        }

        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));
        Set<Long> validItemIds = items.stream().map(OrderItem::getId).collect(Collectors.toSet());
        for (Long itemId : req.itemIds()) {
            if (!validItemIds.contains(itemId)) {
                throw new BusinessException("存在无效的售后商品项");
            }
        }

        Long processingCount = afterSaleRequestMapper.selectCount(
                new LambdaQueryWrapper<AfterSaleRequest>()
                        .eq(AfterSaleRequest::getOrderId, order.getId())
                        .in(AfterSaleRequest::getStatus, List.of("PENDING", "PROCESSING"))
        );
        if (processingCount != null && processingCount > 0) {
            throw new BusinessException("该订单已有售后申请处理中");
        }

        AfterSaleRequest entity = new AfterSaleRequest();
        entity.setOrderId(order.getId());
        entity.setUserId(userId);
        entity.setItemIdsJson(writeJson(req.itemIds()));
        entity.setRequestType(req.requestType());
        entity.setDetailText(req.detailText() == null ? "" : req.detailText().trim());
        entity.setImageUrls(writeJson(req.imageUrls() == null ? List.of() : req.imageUrls()));
        entity.setStatus("PENDING");
        entity.setAdminRemark(null);
        entity.setUserDeleted(false);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        afterSaleRequestMapper.insert(entity);

        order.setStatus("AFTER_SALE");
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        return toDto(entity, order, items, null);
    }

    public List<AfterSaleDTO> listByUser(Long userId) {
        List<AfterSaleRequest> list = afterSaleRequestMapper.selectList(
                new LambdaQueryWrapper<AfterSaleRequest>()
                        .eq(AfterSaleRequest::getUserId, userId)
                        .eq(AfterSaleRequest::getUserDeleted, false)
                        .orderByDesc(AfterSaleRequest::getCreatedAt)
        );
        if (list.isEmpty()) return List.of();
        Set<Long> orderIds = list.stream().map(AfterSaleRequest::getOrderId).collect(Collectors.toSet());
        Map<Long, Order> orderMap = orderMapper.selectBatchIds(orderIds).stream()
                .collect(Collectors.toMap(Order::getId, o -> o));
        Map<Long, List<OrderItem>> itemMap = orderItemMapper.selectList(
                        new LambdaQueryWrapper<OrderItem>().in(OrderItem::getOrderId, orderIds))
                .stream().collect(Collectors.groupingBy(OrderItem::getOrderId));
        return list.stream().map(r -> toDto(r, orderMap.get(r.getOrderId()), itemMap.get(r.getOrderId()), null)).toList();
    }

    public AfterSaleDetailDTO detailByUser(Long userId, Long id) {
        AfterSaleRequest request = afterSaleRequestMapper.selectById(id);
        if (request == null || !Objects.equals(request.getUserId(), userId) || Boolean.TRUE.equals(request.getUserDeleted())) {
            throw new BusinessException("售后记录不存在");
        }
        return buildDetail(request);
    }

    public IPage<AfterSaleDTO> listAdmin(int page, int size, String status, Long orderId, String keyword) {
        LambdaQueryWrapper<AfterSaleRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(AfterSaleRequest::getCreatedAt);
        if (status != null && !status.isBlank()) wrapper.eq(AfterSaleRequest::getStatus, status.trim());
        if (orderId != null && orderId > 0) wrapper.eq(AfterSaleRequest::getOrderId, orderId);
        IPage<AfterSaleRequest> paged = afterSaleRequestMapper.selectPage(new Page<>(page, size), wrapper);
        List<AfterSaleRequest> records = paged.getRecords();
        if (records.isEmpty()) return new Page<AfterSaleDTO>(page, size, paged.getTotal());

        Set<Long> orderIds = records.stream().map(AfterSaleRequest::getOrderId).collect(Collectors.toSet());
        Map<Long, Order> orderMap = orderMapper.selectBatchIds(orderIds).stream()
                .collect(Collectors.toMap(Order::getId, o -> o));
        Set<Long> userIds = orderMap.values().stream().map(Order::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream().collect(Collectors.toMap(User::getId, u -> u));
        Map<Long, List<OrderItem>> itemMap = orderItemMapper.selectList(
                        new LambdaQueryWrapper<OrderItem>().in(OrderItem::getOrderId, orderIds))
                .stream().collect(Collectors.groupingBy(OrderItem::getOrderId));

        List<AfterSaleDTO> dtoRecords = records.stream()
                .map(r -> toDto(r, orderMap.get(r.getOrderId()), itemMap.get(r.getOrderId()), userMap))
                .filter(dto -> {
                    if (keyword == null || keyword.isBlank()) return true;
                    String kw = keyword.trim();
                    return String.valueOf(dto.orderId()).contains(kw) ||
                            String.valueOf(dto.id()).contains(kw) ||
                            (dto.userNickname() != null && dto.userNickname().contains(kw)) ||
                            (dto.userPhone() != null && dto.userPhone().contains(kw));
                })
                .toList();
        Page<AfterSaleDTO> result = new Page<>(page, size, paged.getTotal());
        result.setRecords(dtoRecords);
        return result;
    }

    public AfterSaleDetailDTO detailAdmin(Long id) {
        AfterSaleRequest request = afterSaleRequestMapper.selectById(id);
        if (request == null) throw new BusinessException("售后记录不存在");
        return buildDetail(request);
    }

    @Transactional
    public AfterSaleRequest updateStatus(Long id, String status, String adminRemark) {
        if (!VALID_STATUS.contains(status)) throw new BusinessException("售后状态无效");
        AfterSaleRequest request = afterSaleRequestMapper.selectById(id);
        if (request == null) throw new BusinessException("售后记录不存在");
        request.setStatus(status);
        request.setAdminRemark(adminRemark == null ? null : adminRemark.trim());
        request.setUpdatedAt(LocalDateTime.now());
        afterSaleRequestMapper.updateById(request);
        return request;
    }

    @Transactional
    public void deleteByUser(Long userId, Long id) {
        AfterSaleRequest request = afterSaleRequestMapper.selectById(id);
        if (request == null || !Objects.equals(request.getUserId(), userId)) {
            throw new BusinessException("售后记录不存在");
        }
        if (Boolean.TRUE.equals(request.getUserDeleted())) {
            return;
        }
        if (!"RESOLVED".equals(request.getStatus())) {
            throw new BusinessException("仅已解决售后记录可删除");
        }
        request.setUserDeleted(true);
        request.setUpdatedAt(LocalDateTime.now());
        afterSaleRequestMapper.updateById(request);
    }

    private AfterSaleDetailDTO buildDetail(AfterSaleRequest request) {
        Order order = orderMapper.selectById(request.getOrderId());
        if (order == null) throw new BusinessException("订单不存在");
        OrderDetailResponse orderDetail = orderService.getDetail(order.getId());
        List<Long> selected = readLongList(request.getItemIdsJson());
        List<String> images = readStringList(request.getImageUrls());
        return new AfterSaleDetailDTO(
                toDto(request, order, orderDetail.items(), null),
                orderDetail.order(),
                orderDetail.items(),
                selected,
                images
        );
    }

    private AfterSaleDTO toDto(AfterSaleRequest request, Order order, List<OrderItem> orderItems, Map<Long, User> userMap) {
        List<Long> selectedIds = readLongList(request.getItemIdsJson());
        List<OrderItemSnapshot> selectedItems = (orderItems == null ? List.<OrderItem>of() : orderItems).stream()
                .filter(i -> selectedIds.contains(i.getId()))
                .map(i -> new OrderItemSnapshot(i.getId(), i.getProductId(), i.getProductName(), i.getQuantity()))
                .toList();
        User user = null;
        if (userMap != null && order != null) {
            user = userMap.get(order.getUserId());
        }
        return new AfterSaleDTO(
                request.getId(),
                request.getOrderId(),
                request.getRequestType(),
                request.getDetailText(),
                request.getStatus(),
                request.getAdminRemark(),
                selectedItems,
                order == null ? null : order.getStatus(),
                user == null ? null : user.getNickname(),
                user == null ? null : user.getCompanyName(),
                user == null ? null : user.getPhone(),
                user == null ? null : user.getEmail(),
                request.getCreatedAt(),
                request.getUpdatedAt()
        );
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new BusinessException("数据序列化失败");
        }
    }

    private List<Long> readLongList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<Long>>() {});
        } catch (Exception ex) {
            return List.of();
        }
    }

    private List<String> readStringList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception ex) {
            return List.of();
        }
    }

    public record CreateAfterSaleRequest(
            Long orderId,
            List<Long> itemIds,
            String requestType,
            String detailText,
            List<String> imageUrls
    ) {}

    public record OrderItemSnapshot(
            Long id,
            Long productId,
            String productName,
            Integer quantity
    ) {}

    public record AfterSaleDTO(
            Long id,
            Long orderId,
            String requestType,
            String detailText,
            String status,
            String adminRemark,
            List<OrderItemSnapshot> selectedItems,
            String orderStatus,
            String userNickname,
            String userCompanyName,
            String userPhone,
            String userEmail,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record AfterSaleDetailDTO(
            AfterSaleDTO request,
            com.wonderful.onlineshop.order.dto.AdminOrderDTO order,
            List<OrderItem> orderItems,
            List<Long> selectedItemIds,
            List<String> imageUrls
    ) {}
}
