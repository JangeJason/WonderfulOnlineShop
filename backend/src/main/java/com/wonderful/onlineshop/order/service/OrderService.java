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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
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

    // Valid status transitions
    private static final Set<String> VALID_STATUSES = Set.of(
            "PENDING", "PAID", "SHIPPED", "COMPLETED", "CANCELLED");

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
        Page<Order> orderPage = orderMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<Order>().orderByDesc(Order::getCreatedAt));

        Page<AdminOrderDTO> dtoPage = new Page<>(orderPage.getCurrent(), orderPage.getSize(), orderPage.getTotal());
        List<AdminOrderDTO> dtoList = orderPage.getRecords().stream().map(order -> {
            AdminOrderDTO dto = new AdminOrderDTO(order);
            User user = userMapper.selectById(order.getUserId());
            if (user != null) {
                dto.setUserNickname(user.getNickname());
                dto.setUserCompanyName(user.getCompanyName());
            }

            List<OrderItem> items = orderItemMapper.selectList(
                    new LambdaQueryWrapper<OrderItem>()
                            .eq(OrderItem::getOrderId, order.getId()));

            boolean warning = items.stream().anyMatch(i -> Boolean.TRUE.equals(i.getHasCopyright()) &&
                    (i.getCopyrightFileUrl() == null || i.getCopyrightFileUrl().isEmpty()));
            dto.setHasCopyrightWarning(warning);

            return dto;
        }).collect(Collectors.toList());

        dtoPage.setRecords(dtoList);
        return dtoPage;
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
        }

        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>()
                        .eq(OrderItem::getOrderId, orderId));
        return new OrderDetailResponse(dto, items);
    }

    /** Customer pays: PENDING → PAID */
    public Order pay(Long orderId, Long userId) {
        Order order = getOrderBelongingToUser(orderId, userId);
        if (!"PENDING".equals(order.getStatus())) {
            throw new BusinessException("只有待支付的订单可以支付");
        }
        order.setStatus("PAID");
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
        order.setStatus("COMPLETED");
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

    /** Admin ships: PAID → SHIPPED */
    public Order ship(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null)
            throw new BusinessException("订单不存在");
        if (!"PAID".equals(order.getStatus())) {
            throw new BusinessException("只有已支付的订单可以发货");
        }
        order.setStatus("SHIPPED");
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
        order.setStatus(status);
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
}
