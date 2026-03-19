package com.wonderful.onlineshop.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wonderful.onlineshop.cart.entity.CartItem;
import com.wonderful.onlineshop.cart.service.CartService;
import com.wonderful.onlineshop.common.BusinessException;
import com.wonderful.onlineshop.order.dto.AdminOrderDTO;
import com.wonderful.onlineshop.order.dto.OrderDetailResponse;
import com.wonderful.onlineshop.order.dto.OrderListDTO;
import com.wonderful.onlineshop.order.entity.Order;
import com.wonderful.onlineshop.order.entity.OrderItem;
import com.wonderful.onlineshop.order.mapper.OrderItemMapper;
import com.wonderful.onlineshop.order.mapper.OrderMapper;
import com.wonderful.onlineshop.product.entity.Product;
import com.wonderful.onlineshop.product.mapper.ProductMapper;
import com.wonderful.onlineshop.user.entity.Address;
import com.wonderful.onlineshop.user.entity.User;
import com.wonderful.onlineshop.user.mapper.AddressMapper;
import com.wonderful.onlineshop.user.mapper.UserMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final CartService cartService;
    private final ProductMapper productMapper;
    private final UserMapper userMapper;
    private final AddressMapper addressMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Valid order statuses
    private static final Set<String> VALID_STATUSES = Set.of(
            "PENDING", "WAIT_PRODUCTION", "IN_PRODUCTION", "WAIT_SHIPMENT", "SHIPPED", "COMPLETED", "AFTER_SALE",
            "REJECTED",
            "CANCELLED");

    private static final Set<String> VALID_SHIPPING_STATUSES = Set.of("IN_TRANSIT", "OUT_FOR_DELIVERY", "SIGNED");
    private static final List<String> PRODUCTION_STATUS_FLOW = List.of(
            "PREPRESS_CHECK",
            "PLATE_MAKING",
            "PRINTING",
            "POST_PROCESS",
            "QC_PACKING");
    private static final Set<String> VALID_PRODUCTION_STATUSES = Set.copyOf(PRODUCTION_STATUS_FLOW);

    // Standardized main flow:
    // PENDING -> WAIT_PRODUCTION -> IN_PRODUCTION -> WAIT_SHIPMENT -> SHIPPED -> COMPLETED -> AFTER_SALE
    // CANCELLED is terminal.
    private static final Map<String, Set<String>> ALLOWED_STATUS_TRANSITIONS = Map.of(
            "PENDING", Set.of("WAIT_PRODUCTION"),
            "WAIT_PRODUCTION", Set.of("IN_PRODUCTION"),
            "IN_PRODUCTION", Set.of("WAIT_SHIPMENT"),
            "WAIT_SHIPMENT", Set.of("SHIPPED"),
            "SHIPPED", Set.of("COMPLETED", "AFTER_SALE"),
            "COMPLETED", Set.of("AFTER_SALE"),
            "AFTER_SALE", Set.of(),
            "REJECTED", Set.of("WAIT_PRODUCTION"),
            "CANCELLED", Set.of());

    public OrderService(OrderMapper orderMapper,
            OrderItemMapper orderItemMapper,
            CartService cartService,
            ProductMapper productMapper,
            UserMapper userMapper,
            AddressMapper addressMapper) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.cartService = cartService;
        this.productMapper = productMapper;
        this.userMapper = userMapper;
        this.addressMapper = addressMapper;
    }

    @Transactional
    public Order createFromCart(Long userId, String remark, List<Long> cartItemIds, Long addressId) {
        if (cartItemIds == null || cartItemIds.isEmpty()) {
            throw new BusinessException("未选择任何商品，无法下单");
        }

        List<CartItem> allCartItems = cartService.listByUser(userId);
        List<CartItem> cartItems = allCartItems.stream()
                .filter(ci -> cartItemIds.contains(ci.getId()))
                .collect(Collectors.toList());

        if (cartItems.isEmpty()) {
            throw new BusinessException("购物车中所选商品无效或为空，无法下单");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (CartItem ci : cartItems) {
            BigDecimal lineTotal = ci.getUnitPrice().multiply(new BigDecimal(ci.getQuantity()));
            lineTotal = lineTotal.add(extractAppliedSetupFee(ci.getParamsSnapshot(), ci.getQuantity()));
            totalAmount = totalAmount.add(lineTotal);
        }

        Order order = new Order();
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setStatus("PENDING");
        order.setProductionStatus(null);
        order.setShippingStatus(null);
        order.setReviewStatus("PENDING");
        order.setReviewReason(null);
        order.setReviewedBy(null);
        order.setReviewedAt(null);
        order.setRemark(remark);

        if (addressId != null) {
            Address address = addressMapper.selectById(addressId);
            if (address != null) {
                order.setReceiverName(address.getReceiverName());
                order.setReceiverPhone(address.getPhone());
                order.setReceiverAddress(
                        address.getProvince() + address.getCity() + address.getDistrict() + address.getDetailAddress());
            }
        }

        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.insert(order);

        for (CartItem ci : cartItems) {
            Product product = productMapper.selectById(ci.getProductId());
            String productName = product != null ? product.getName() : "未知商品";

            OrderItem oi = new OrderItem();
            oi.setOrderId(order.getId());
            oi.setProductId(ci.getProductId());
            oi.setProductName(productName);
            oi.setQuantity(ci.getQuantity());
            oi.setUnitPrice(ci.getUnitPrice());
            oi.setParamsSnapshot(ci.getParamsSnapshot());
            oi.setPrintFileUrl(ci.getPrintFileUrl());
            oi.setProofFileUrl(ci.getProofFileUrl());
            oi.setHasCopyright(ci.getHasCopyright() != null ? ci.getHasCopyright() : false);
            oi.setCopyrightFileUrl(ci.getCopyrightFileUrl());
            orderItemMapper.insert(oi);
        }

        cartService.removeBatch(cartItemIds, userId);
        return order;
    }

    public List<OrderListDTO> listByUser(Long userId) {
        List<Order> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getUserId, userId)
                        .orderByDesc(Order::getCreatedAt));

        return orders.stream().map(order -> {
            OrderListDTO dto = new OrderListDTO(order);
            List<OrderItem> items = orderItemMapper.selectList(
                    new LambdaQueryWrapper<OrderItem>()
                            .eq(OrderItem::getOrderId, order.getId()));

            // Limit to at most 3 items for preview
            dto.setPreviewItems(items.size() > 3 ? items.subList(0, 3) : items);
            // Show how many product lines are in the order, not summed item quantity.
            dto.setTotalProductCount(items.size());

            boolean warning = items.stream().anyMatch(i -> Boolean.TRUE.equals(i.getHasCopyright()) &&
                    (i.getCopyrightFileUrl() == null || i.getCopyrightFileUrl().isEmpty()));
            dto.setHasCopyrightWarning(warning);

            return dto;
        }).collect(Collectors.toList());
    }

    public List<Order> listAll() {
        return orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .orderByDesc(Order::getCreatedAt));
    }

    public IPage<AdminOrderDTO> listAllPaged(int page, int size) {
        return listAllPaged(page, size, null);
    }

    public IPage<AdminOrderDTO> listAllPaged(int page, int size, AdminOrderFilter filter) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Order::getCreatedAt);

        if (filter != null) {
            if (filter.orderId() != null && filter.orderId() > 0) {
                wrapper.eq(Order::getId, filter.orderId());
            }
            if (filter.status() != null && !filter.status().isBlank()) {
                wrapper.eq(Order::getStatus, filter.status().trim());
            }
            if (filter.createdFrom() != null) {
                wrapper.ge(Order::getCreatedAt, filter.createdFrom().atStartOfDay());
            }
            if (filter.createdTo() != null) {
                wrapper.le(Order::getCreatedAt, filter.createdTo().plusDays(1).atStartOfDay().minusNanos(1));
            }
            if (filter.regionKeyword() != null && !filter.regionKeyword().isBlank()) {
                wrapper.like(Order::getReceiverAddress, filter.regionKeyword().trim());
            }
            if (filter.urgent() != null) {
                if (Boolean.TRUE.equals(filter.urgent())) {
                    wrapper.like(Order::getRemark, "加急");
                } else {
                    wrapper.and(w -> w.isNull(Order::getRemark).or().notLike(Order::getRemark, "加急"));
                }
            }
            if (filter.customerKeyword() != null && !filter.customerKeyword().isBlank()) {
                String keyword = filter.customerKeyword().trim();
                List<Long> matchedUserIds = userMapper.selectList(
                                new LambdaQueryWrapper<User>()
                                        .select(User::getId)
                                        .and(w -> w.like(User::getNickname, keyword)
                                                .or()
                                                .like(User::getCompanyName, keyword)
                                                .or()
                                                .like(User::getPhone, keyword)
                                                .or()
                                                .like(User::getUsername, keyword)))
                        .stream()
                        .map(User::getId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                wrapper.and(w -> {
                    w.like(Order::getReceiverName, keyword)
                            .or()
                            .like(Order::getReceiverPhone, keyword);
                    if (!matchedUserIds.isEmpty()) {
                        w.or().in(Order::getUserId, matchedUserIds);
                    }
                });
            }
            if (filter.hasCopyrightFile() != null) {
                List<Long> orderIdsWithCopyrightFile = orderItemMapper.selectList(
                                new LambdaQueryWrapper<OrderItem>()
                                        .select(OrderItem::getOrderId)
                                        .eq(OrderItem::getHasCopyright, true)
                                        .isNotNull(OrderItem::getCopyrightFileUrl)
                                        .ne(OrderItem::getCopyrightFileUrl, ""))
                        .stream()
                        .map(OrderItem::getOrderId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .collect(Collectors.toList());
                if (Boolean.TRUE.equals(filter.hasCopyrightFile())) {
                    if (orderIdsWithCopyrightFile.isEmpty()) {
                        return new Page<>(page, size, 0);
                    }
                    wrapper.in(Order::getId, orderIdsWithCopyrightFile);
                } else if (!orderIdsWithCopyrightFile.isEmpty()) {
                    wrapper.notIn(Order::getId, orderIdsWithCopyrightFile);
                }
            }
        }

        Page<Order> orderPage = orderMapper.selectPage(
                new Page<>(page, size),
                wrapper);

        Page<AdminOrderDTO> dtoPage = new Page<>(orderPage.getCurrent(), orderPage.getSize(), orderPage.getTotal());
        List<AdminOrderDTO> dtoList = orderPage.getRecords().stream().map(order -> {
            AdminOrderDTO dto = new AdminOrderDTO(order);
            User user = userMapper.selectById(order.getUserId());
            if (user != null) {
                dto.setUserNickname(user.getNickname());
                dto.setUserCompanyName(user.getCompanyName());
                dto.setUserPhone(user.getPhone());
                dto.setUserEmail(user.getEmail());
            }

            List<OrderItem> items = orderItemMapper.selectList(
                    new LambdaQueryWrapper<OrderItem>()
                            .eq(OrderItem::getOrderId, order.getId()));

            boolean hasCopyrightIssue = items.stream().anyMatch(i -> Boolean.TRUE.equals(i.getHasCopyright()));
            boolean warning = items.stream().anyMatch(i -> Boolean.TRUE.equals(i.getHasCopyright()) &&
                    (i.getCopyrightFileUrl() == null || i.getCopyrightFileUrl().isEmpty()));
            dto.setHasCopyrightIssue(hasCopyrightIssue);
            dto.setHasCopyrightWarning(warning);
            dto.setUrgent(order.getRemark() != null && order.getRemark().contains("加急"));
            dto.setPreviewProductNames(
                    items.stream()
                            .map(OrderItem::getProductName)
                            .filter(Objects::nonNull)
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .distinct()
                            .limit(5)
                            .collect(Collectors.toList())
            );
            dto.setPreviewProductCount(items.size());

            return dto;
        }).collect(Collectors.toList());

        dtoPage.setRecords(dtoList);
        return dtoPage;
    }

    public record AdminOrderFilter(
            Long orderId,
            String status,
            String customerKeyword,
            String regionKeyword,
            Boolean hasCopyrightFile,
            Boolean urgent,
            LocalDate createdFrom,
            LocalDate createdTo
    ) {
    }

    public OrderDetailResponse getDetail(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        AdminOrderDTO dto = new AdminOrderDTO(order);
        User user = userMapper.selectById(order.getUserId());
        if (user != null) {
            dto.setUserNickname(user.getNickname());
            dto.setUserCompanyName(user.getCompanyName());
            dto.setUserPhone(user.getPhone());
            dto.setUserEmail(user.getEmail());
        }

        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>()
                        .eq(OrderItem::getOrderId, orderId));
        boolean hasCopyrightIssue = items.stream().anyMatch(i -> Boolean.TRUE.equals(i.getHasCopyright()));
        boolean warning = items.stream().anyMatch(i -> Boolean.TRUE.equals(i.getHasCopyright()) &&
                (i.getCopyrightFileUrl() == null || i.getCopyrightFileUrl().isEmpty()));
        dto.setHasCopyrightIssue(hasCopyrightIssue);
        dto.setHasCopyrightWarning(warning);
        dto.setUrgent(order.getRemark() != null && order.getRemark().contains("加急"));

        return new OrderDetailResponse(dto, items);
    }

    /** Customer pays: PENDING → WAIT_PRODUCTION */
    public Order pay(Long orderId, Long userId) {
        Order order = getOrderBelongingToUser(orderId, userId);
        if (!"PENDING".equals(order.getStatus())) {
            throw new BusinessException("只有待支付的订单可以支付");
        }
        order.setStatus("WAIT_PRODUCTION");
        order.setReviewStatus("PENDING");
        order.setReviewReason(null);
        order.setReviewedBy(null);
        order.setReviewedAt(null);
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        return order;
    }

    /** Customer cancels: PENDING → CANCELLED */
    public Order cancel(Long orderId, Long userId) {
        Order order = getOrderBelongingToUser(orderId, userId);
        if (!"PENDING".equals(order.getStatus())) {
            throw new BusinessException("只有待支付的订单可以取消");
        }
        order.setStatus("CANCELLED");
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        return order;
    }

    /** Customer confirms receipt: SHIPPED → COMPLETED */
    public Order confirmReceive(Long orderId, Long userId) {
        Order order = getOrderBelongingToUser(orderId, userId);
        if (!"SHIPPED".equals(order.getStatus())) {
            throw new BusinessException("只有已发货的订单可以确认收货");
        }
        if (!"SIGNED".equals(order.getShippingStatus())) {
            throw new BusinessException("物流未签收，暂不能确认收货");
        }
        order.setStatus("COMPLETED");
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        return order;
    }

    /** Customer applies after-sale: (SHIPPED + SIGNED) or COMPLETED -> AFTER_SALE */
    public Order applyAfterSale(Long orderId, Long userId) {
        Order order = getOrderBelongingToUser(orderId, userId);
        boolean fromCompleted = "COMPLETED".equals(order.getStatus());
        boolean fromSignedShipped = "SHIPPED".equals(order.getStatus()) && "SIGNED".equals(order.getShippingStatus());
        if (!fromCompleted && !fromSignedShipped) {
            throw new BusinessException("仅已签收或已完成订单可申请售后");
        }
        order.setStatus("AFTER_SALE");
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        return order;
    }

    /** Customer resubmits rejected order for production review */
    public Order resubmitReview(Long orderId, Long userId) {
        Order order = getOrderBelongingToUser(orderId, userId);
        if (!"REJECTED".equals(order.getStatus())) {
            throw new BusinessException("仅已驳回订单可重新提交审核");
        }
        order.setStatus("WAIT_PRODUCTION");
        order.setReviewStatus("PENDING");
        order.setReviewReason(null);
        order.setReviewedBy(null);
        order.setReviewedAt(null);
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        return order;
    }

    /** Customer updates custom name */
    public Order updateCustomName(Long orderId, Long userId, String customName) {
        Order order = getOrderBelongingToUser(orderId, userId);
        order.setCustomName(customName);
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        return order;
    }

    @Transactional
    public int reorderToCart(Long orderId, Long userId) {
        Order order = getOrderBelongingToUser(orderId, userId);
        if (!"CANCELLED".equals(order.getStatus())) {
            throw new BusinessException("只有已取消订单可重新下单");
        }
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
        if (items.isEmpty()) {
            throw new BusinessException("订单中无可重新下单商品");
        }
        for (OrderItem oi : items) {
            CartItem ci = new CartItem();
            ci.setUserId(userId);
            ci.setProductId(oi.getProductId());
            ci.setQuantity(oi.getQuantity());
            ci.setParamsSnapshot(oi.getParamsSnapshot());
            ci.setUnitPrice(oi.getUnitPrice());
            ci.setPrintFileUrl(oi.getPrintFileUrl());
            ci.setProofFileUrl(oi.getProofFileUrl());
            ci.setHasCopyright(oi.getHasCopyright() != null ? oi.getHasCopyright() : false);
            ci.setCopyrightFileUrl(oi.getCopyrightFileUrl());
            ci.setCreatedAt(LocalDateTime.now());
            cartService.addItem(ci);
        }
        return items.size();
    }

    @Transactional
    public void deleteCancelledOrder(Long orderId, Long userId) {
        Order order = getOrderBelongingToUser(orderId, userId);
        if (!"CANCELLED".equals(order.getStatus())) {
            throw new BusinessException("只有已取消订单可删除");
        }
        orderItemMapper.delete(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
        orderMapper.deleteById(orderId);
    }

    @Transactional
    public OrderItem updateRejectedOrderItem(
            Long orderId,
            Long itemId,
            Long userId,
            Integer quantity,
            String paramsSnapshot,
            BigDecimal unitPrice,
            String printFileUrl,
            String proofFileUrl,
            Boolean hasCopyright,
            String copyrightFileUrl
    ) {
        Order order = getOrderBelongingToUser(orderId, userId);
        if (!"REJECTED".equals(order.getStatus())) {
            throw new BusinessException("仅待修改订单可编辑");
        }
        OrderItem item = orderItemMapper.selectById(itemId);
        if (item == null || !orderId.equals(item.getOrderId())) {
            throw new BusinessException("订单商品不存在");
        }
        if (quantity == null || quantity <= 0) {
            throw new BusinessException("数量必须大于0");
        }
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("单价无效");
        }
        item.setQuantity(quantity);
        item.setParamsSnapshot(paramsSnapshot);
        item.setUnitPrice(unitPrice);
        item.setPrintFileUrl(printFileUrl);
        item.setProofFileUrl(proofFileUrl);
        item.setHasCopyright(hasCopyright != null ? hasCopyright : false);
        item.setCopyrightFileUrl(copyrightFileUrl);
        orderItemMapper.updateById(item);

        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (OrderItem oi : items) {
            BigDecimal line = oi.getUnitPrice().multiply(new BigDecimal(oi.getQuantity()));
            line = line.add(extractAppliedSetupFee(oi.getParamsSnapshot(), oi.getQuantity()));
            totalAmount = totalAmount.add(line);
        }
        order.setTotalAmount(totalAmount);
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        return item;
    }

    @Transactional
    public OrderItem updateOrderItemFiles(
            Long orderId,
            Long itemId,
            Long userId,
            String printFileUrl,
            String printFileName,
            String proofFileUrl,
            String proofFileName
    ) {
        Order order = getOrderBelongingToUser(orderId, userId);
        if (!"PENDING".equals(order.getStatus()) && !"WAIT_PRODUCTION".equals(order.getStatus())) {
            throw new BusinessException("仅待付款或待生产订单可补传文件");
        }
        OrderItem item = orderItemMapper.selectById(itemId);
        if (item == null || !orderId.equals(item.getOrderId())) {
            throw new BusinessException("订单商品不存在");
        }
        if (printFileUrl != null && !printFileUrl.isBlank()) {
            item.setPrintFileUrl(printFileUrl.trim());
        }
        if (proofFileUrl != null && !proofFileUrl.isBlank()) {
            item.setProofFileUrl(proofFileUrl.trim());
        }
        item.setParamsSnapshot(mergeSnapshotFileNames(item.getParamsSnapshot(), printFileName, proofFileName));
        orderItemMapper.updateById(item);

        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        return item;
    }

    /** Admin ships: WAIT_SHIPMENT → SHIPPED */
    public Order ship(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null)
            throw new BusinessException("订单不存在");
        if (!"WAIT_SHIPMENT".equals(order.getStatus())) {
            throw new BusinessException("只有待发货的订单可以发货");
        }
        order.setStatus("SHIPPED");
        order.setShippingStatus("IN_TRANSIT");
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        return order;
    }

    /** Admin approves review and enters production from first production sub-status */
    public Order approveToProduction(Long orderId, Long adminUserId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!"WAIT_PRODUCTION".equals(order.getStatus())) {
            throw new BusinessException("仅待生产订单可审核通过进入生产");
        }
        order.setStatus("IN_PRODUCTION");
        order.setProductionStatus(PRODUCTION_STATUS_FLOW.get(0));
        order.setReviewStatus("APPROVED");
        order.setReviewReason(null);
        order.setReviewedBy(adminUserId);
        order.setReviewedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        return order;
    }

    /** Admin rejects production review with mandatory reason */
    public Order rejectProduction(Long orderId, Long adminUserId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessException("驳回原因不能为空");
        }
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!"WAIT_PRODUCTION".equals(order.getStatus())) {
            throw new BusinessException("仅待生产订单可驳回");
        }
        order.setStatus("REJECTED");
        order.setProductionStatus(null);
        order.setShippingStatus(null);
        order.setReviewStatus("REJECTED");
        order.setReviewReason(reason.trim());
        order.setReviewedBy(adminUserId);
        order.setReviewedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        return order;
    }

    /** Admin moves production sub-status to next stage; last stage moves order to WAIT_SHIPMENT */
    public Order advanceProductionStatus(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!"IN_PRODUCTION".equals(order.getStatus())) {
            throw new BusinessException("仅生产中订单可推进生产环节");
        }
        String current = order.getProductionStatus();
        if (current == null || current.isBlank()) {
            current = PRODUCTION_STATUS_FLOW.get(0);
        }
        if (!VALID_PRODUCTION_STATUSES.contains(current)) {
            throw new BusinessException("无效的生产子状态: " + current);
        }
        int idx = PRODUCTION_STATUS_FLOW.indexOf(current);
        if (idx < 0) {
            throw new BusinessException("无效的生产子状态: " + current);
        }
        if (idx >= PRODUCTION_STATUS_FLOW.size() - 1) {
            order.setStatus("WAIT_SHIPMENT");
            order.setProductionStatus(null);
        } else {
            order.setProductionStatus(PRODUCTION_STATUS_FLOW.get(idx + 1));
        }
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        return order;
    }

    /** Admin updates shipping sub-status for SHIPPED orders */
    public Order updateShippingStatus(Long orderId, String shippingStatus) {
        if (!VALID_SHIPPING_STATUSES.contains(shippingStatus)) {
            throw new BusinessException("无效的物流状态: " + shippingStatus);
        }
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!"SHIPPED".equals(order.getStatus())) {
            throw new BusinessException("只有已发货订单可更新物流状态");
        }
        order.setShippingStatus(shippingStatus);
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        return order;
    }

    /** Admin directly sets production sub-status for IN_PRODUCTION orders (supports forward/backward adjustment). */
    public Order updateProductionStatus(Long orderId, String productionStatus) {
        if (!VALID_PRODUCTION_STATUSES.contains(productionStatus)) {
            throw new BusinessException("无效的生产子状态: " + productionStatus);
        }
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!"IN_PRODUCTION".equals(order.getStatus())) {
            throw new BusinessException("只有生产中订单可更新生产环节");
        }
        order.setProductionStatus(productionStatus);
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        return order;
    }

    /** Admin updates status (with validation) */
    public Order updateStatus(Long orderId, String status) {
        if (!VALID_STATUSES.contains(status)) {
            throw new BusinessException("无效的订单状态: " + status);
        }
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        String current = order.getStatus();
        if (!Objects.equals(current, status)) {
            Set<String> allowedNext = ALLOWED_STATUS_TRANSITIONS.getOrDefault(current, Collections.emptySet());
            if (!allowedNext.contains(status)) {
                throw new BusinessException("不允许的状态流转: " + current + " -> " + status);
            }
            if ("AFTER_SALE".equals(status) && "SHIPPED".equals(current) && !"SIGNED".equals(order.getShippingStatus())) {
                throw new BusinessException("已发货订单仅在已签收后可进入售后中");
            }
            if ("IN_PRODUCTION".equals(status) && (order.getReviewStatus() == null || !"APPROVED".equals(order.getReviewStatus()))) {
                throw new BusinessException("待生产订单需审核通过后方可进入生产中");
            }
        }
        order.setStatus(status);
        if ("SHIPPED".equals(status)) {
            if (order.getShippingStatus() == null || order.getShippingStatus().isBlank()) {
                order.setShippingStatus("IN_TRANSIT");
            }
            order.setProductionStatus(null);
        } else if ("IN_PRODUCTION".equals(status)) {
            if (order.getProductionStatus() == null || order.getProductionStatus().isBlank()) {
                order.setProductionStatus(PRODUCTION_STATUS_FLOW.get(0));
            }
            order.setShippingStatus(null);
        } else {
            if (!"WAIT_PRODUCTION".equals(status)) {
                order.setProductionStatus(null);
            }
            order.setShippingStatus(null);
        }
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        return order;
    }

    private Order getOrderBelongingToUser(Long orderId, Long userId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException("订单不存在");
        }
        return order;
    }

    private BigDecimal extractAppliedSetupFee(String paramsSnapshot, Integer quantity) {
        if (paramsSnapshot == null || paramsSnapshot.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            JsonNode root = objectMapper.readTree(paramsSnapshot);
            BigDecimal setupFee = BigDecimal.ZERO;
            JsonNode feeNode = root.get("setupFee");
            if (feeNode != null && !feeNode.isNull()) {
                if (feeNode.isNumber()) {
                    setupFee = feeNode.decimalValue();
                } else if (feeNode.isTextual()) {
                    String text = feeNode.asText();
                    if (text != null && !text.isBlank()) {
                        setupFee = new BigDecimal(text);
                    }
                }
            }

            if (setupFee.compareTo(BigDecimal.ZERO) <= 0) {
                return BigDecimal.ZERO;
            }

            int freeSetupQuantity = 0;
            JsonNode freeQtyNode = root.get("freeSetupQuantity");
            if (freeQtyNode != null && !freeQtyNode.isNull()) {
                if (freeQtyNode.isInt() || freeQtyNode.isLong() || freeQtyNode.isNumber()) {
                    freeSetupQuantity = freeQtyNode.asInt();
                } else if (freeQtyNode.isTextual()) {
                    String text = freeQtyNode.asText();
                    if (text != null && !text.isBlank()) {
                        freeSetupQuantity = Integer.parseInt(text);
                    }
                }
            }

            if (freeSetupQuantity <= 0) {
                return setupFee;
            }
            int safeQuantity = quantity == null ? 0 : quantity;
            if (safeQuantity < freeSetupQuantity) {
                return setupFee;
            }
            return BigDecimal.ZERO;
        } catch (NumberFormatException ignored) {
            // Ignore invalid numeric legacy values and treat setup fee as zero.
        } catch (Exception ignored) {
            // Ignore invalid legacy snapshot format and treat setup fee as zero.
        }
        return BigDecimal.ZERO;
    }

    private String mergeSnapshotFileNames(String paramsSnapshot, String printFileName, String proofFileName) {
        try {
            ObjectNode node;
            if (paramsSnapshot == null || paramsSnapshot.isBlank()) {
                node = objectMapper.createObjectNode();
            } else {
                JsonNode parsed = objectMapper.readTree(paramsSnapshot);
                node = parsed instanceof ObjectNode ? (ObjectNode) parsed : objectMapper.createObjectNode();
            }
            if (printFileName != null && !printFileName.isBlank()) {
                node.put("printFileName", printFileName.trim());
            }
            if (proofFileName != null && !proofFileName.isBlank()) {
                node.put("proofFileName", proofFileName.trim());
            }
            return objectMapper.writeValueAsString(node);
        } catch (Exception ex) {
            return paramsSnapshot;
        }
    }
}
