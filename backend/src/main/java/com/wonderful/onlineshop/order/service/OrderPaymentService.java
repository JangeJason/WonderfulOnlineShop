package com.wonderful.onlineshop.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wonderful.onlineshop.common.BusinessException;
import com.wonderful.onlineshop.order.dto.OrderPayCreateResponse;
import com.wonderful.onlineshop.order.entity.Order;
import com.wonderful.onlineshop.order.entity.OrderItem;
import com.wonderful.onlineshop.order.mapper.OrderItemMapper;
import com.wonderful.onlineshop.order.mapper.OrderMapper;
import com.wonderful.onlineshop.user.service.WechatAuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.UUID;

@Service
public class OrderPaymentService {

    private static final String WECHAT_PAY_BASE = "https://api.mch.weixin.qq.com";
    private static final ZoneId CN_ZONE = ZoneId.of("Asia/Shanghai");

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final WechatAuthService wechatAuthService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${wechat.miniapp.appid:}")
    private String appId;

    @Value("${wechat.pay.enabled:false}")
    private boolean payEnabled;

    @Value("${wechat.pay.mch-id:}")
    private String mchId;

    @Value("${wechat.pay.serial-no:}")
    private String serialNo;

    @Value("${wechat.pay.private-key-path:}")
    private String privateKeyPath;

    @Value("${wechat.pay.api-v3-key:}")
    private String apiV3Key;

    @Value("${wechat.pay.notify-url:}")
    private String notifyUrl;

    private volatile PrivateKey merchantPrivateKey;

    public OrderPaymentService(
            OrderMapper orderMapper,
            OrderItemMapper orderItemMapper,
            WechatAuthService wechatAuthService
    ) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.wechatAuthService = wechatAuthService;
    }

    @Transactional
    public OrderPayCreateResponse createPayment(Long orderId, Long userId, String channel, String jsCode, String clientIp) {
        ensurePayConfigReady();
        String normalizedChannel = normalizeChannel(channel);
        Order order = getOrderBelongingToUser(orderId, userId);
        if (!"PENDING".equals(order.getStatus())) {
            throw new BusinessException("仅待支付订单可发起支付");
        }

        if (order.getOutTradeNo() == null || order.getOutTradeNo().isBlank()) {
            order.setOutTradeNo(buildOutTradeNo(order.getId()));
        }
        order.setPayChannel(normalizedChannel);
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);

        String description = buildPaymentDescription(order.getId());
        int totalFen = toFen(order.getTotalAmount());

        if ("JSAPI".equals(normalizedChannel)) {
            String openid = wechatAuthService.resolveOpenIdByJsCode(jsCode);
            String prepayId = createJsapiPrepay(order.getOutTradeNo(), description, totalFen, openid, resolveClientIp(clientIp));
            return buildJsapiResponse(order.getOutTradeNo(), prepayId);
        }

        String codeUrl = createNativePrepay(order.getOutTradeNo(), description, totalFen, resolveClientIp(clientIp));
        OrderPayCreateResponse response = new OrderPayCreateResponse();
        response.setChannel("NATIVE");
        response.setOutTradeNo(order.getOutTradeNo());
        response.setCodeUrl(codeUrl);
        return response;
    }

    @Transactional
    public Order syncPayment(Long orderId, Long userId) {
        ensurePayConfigReady();
        Order order = getOrderBelongingToUser(orderId, userId);
        if (!"PENDING".equals(order.getStatus())) {
            return order;
        }
        if (order.getOutTradeNo() == null || order.getOutTradeNo().isBlank()) {
            return order;
        }

        JsonNode query = queryByOutTradeNo(order.getOutTradeNo());
        String tradeState = query.path("trade_state").asText("");
        if ("SUCCESS".equalsIgnoreCase(tradeState)) {
            markOrderPaid(order, query.path("trade_type").asText(order.getPayChannel()), query.path("success_time").asText(null));
        }
        return orderMapper.selectById(orderId);
    }

    @Transactional
    public boolean handleWechatNotify(String body) {
        ensurePayConfigReady();
        try {
            JsonNode root = objectMapper.readTree(body == null ? "{}" : body);
            JsonNode resource = root.path("resource");
            if (resource.isMissingNode() || resource.isNull()) {
                return false;
            }
            String plaintext = decryptNotifyResource(
                    resource.path("ciphertext").asText(""),
                    resource.path("nonce").asText(""),
                    resource.path("associated_data").asText("")
            );
            JsonNode notifyData = objectMapper.readTree(plaintext);
            String outTradeNo = notifyData.path("out_trade_no").asText("");
            String tradeState = notifyData.path("trade_state").asText("");
            if (outTradeNo.isBlank()) {
                return false;
            }
            if (!"SUCCESS".equalsIgnoreCase(tradeState)) {
                return true;
            }
            Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOutTradeNo, outTradeNo).last("LIMIT 1"));
            if (order == null) {
                return true;
            }
            markOrderPaid(order, notifyData.path("trade_type").asText(order.getPayChannel()), notifyData.path("success_time").asText(null));
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private OrderPayCreateResponse buildJsapiResponse(String outTradeNo, String prepayId) {
        String timeStamp = String.valueOf(Instant.now().getEpochSecond());
        String nonceStr = randomNonce();
        String packageValue = "prepay_id=" + prepayId;
        String signType = "RSA";
        String message = appId + "\n" + timeStamp + "\n" + nonceStr + "\n" + packageValue + "\n";
        String paySign = signMessage(message);

        OrderPayCreateResponse.JsapiParams params = new OrderPayCreateResponse.JsapiParams();
        params.setTimeStamp(timeStamp);
        params.setNonceStr(nonceStr);
        params.setPackageValue(packageValue);
        params.setSignType(signType);
        params.setPaySign(paySign);

        OrderPayCreateResponse response = new OrderPayCreateResponse();
        response.setChannel("JSAPI");
        response.setOutTradeNo(outTradeNo);
        response.setJsapiParams(params);
        return response;
    }

    private String createJsapiPrepay(String outTradeNo, String description, int totalFen, String openid, String clientIp) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("appid", appId);
        payload.put("mchid", mchId);
        payload.put("description", description);
        payload.put("out_trade_no", outTradeNo);
        payload.put("notify_url", notifyUrl);
        ObjectNode amount = payload.putObject("amount");
        amount.put("total", totalFen);
        amount.put("currency", "CNY");
        ObjectNode payer = payload.putObject("payer");
        payer.put("openid", openid);
        ObjectNode sceneInfo = payload.putObject("scene_info");
        sceneInfo.put("payer_client_ip", clientIp);
        JsonNode response = doWechatRequest(HttpMethod.POST, "/v3/pay/transactions/jsapi", payload.toString());
        String prepayId = response.path("prepay_id").asText("");
        if (prepayId.isBlank()) {
            throw new BusinessException("微信下单失败：未返回 prepay_id");
        }
        return prepayId;
    }

    private String createNativePrepay(String outTradeNo, String description, int totalFen, String clientIp) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("appid", appId);
        payload.put("mchid", mchId);
        payload.put("description", description);
        payload.put("out_trade_no", outTradeNo);
        payload.put("notify_url", notifyUrl);
        ObjectNode amount = payload.putObject("amount");
        amount.put("total", totalFen);
        amount.put("currency", "CNY");
        ObjectNode sceneInfo = payload.putObject("scene_info");
        sceneInfo.put("payer_client_ip", clientIp);
        JsonNode response = doWechatRequest(HttpMethod.POST, "/v3/pay/transactions/native", payload.toString());
        String codeUrl = response.path("code_url").asText("");
        if (codeUrl.isBlank()) {
            throw new BusinessException("微信下单失败：未返回 code_url");
        }
        return codeUrl;
    }

    private JsonNode queryByOutTradeNo(String outTradeNo) {
        String encoded = java.net.URLEncoder.encode(outTradeNo, StandardCharsets.UTF_8);
        String path = "/v3/pay/transactions/out-trade-no/" + encoded + "?mchid=" + mchId;
        return doWechatRequest(HttpMethod.GET, path, "");
    }

    private JsonNode doWechatRequest(HttpMethod method, String urlPathWithQuery, String body) {
        String nonceStr = randomNonce();
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String normalizedBody = body == null ? "" : body;
        String message = method.name() + "\n"
                + urlPathWithQuery + "\n"
                + timestamp + "\n"
                + nonceStr + "\n"
                + normalizedBody + "\n";
        String signature = signMessage(message);
        String authorization = String.format(
                "WECHATPAY2-SHA256-RSA2048 mchid=\"%s\",nonce_str=\"%s\",timestamp=\"%s\",serial_no=\"%s\",signature=\"%s\"",
                mchId, nonceStr, timestamp, serialNo, signature);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", "application/json");
        headers.set("Authorization", authorization);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    WECHAT_PAY_BASE + urlPathWithQuery,
                    method,
                    new HttpEntity<>(normalizedBody, headers),
                    String.class
            );
            return objectMapper.readTree(response.getBody() == null ? "{}" : response.getBody());
        } catch (HttpStatusCodeException ex) {
            String respBody = ex.getResponseBodyAsString();
            String msg = "微信支付请求失败: HTTP " + ex.getStatusCode().value()
                    + (respBody == null || respBody.isBlank() ? "" : ", body=" + respBody);
            throw new BusinessException(msg);
        } catch (Exception ex) {
            throw new BusinessException("微信支付请求失败: " + ex.getMessage());
        }
    }

    private synchronized PrivateKey getMerchantPrivateKey() {
        if (merchantPrivateKey != null) {
            return merchantPrivateKey;
        }
        try {
            String pem = Files.readString(Path.of(privateKeyPath), StandardCharsets.UTF_8);
            String normalized = pem
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] der = Base64.getDecoder().decode(normalized);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(der);
            merchantPrivateKey = KeyFactory.getInstance("RSA").generatePrivate(spec);
            return merchantPrivateKey;
        } catch (Exception ex) {
            throw new BusinessException("读取微信支付私钥失败: " + ex.getMessage());
        }
    }

    private String signMessage(String message) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(getMerchantPrivateKey());
            signature.update(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception ex) {
            throw new BusinessException("微信支付签名失败: " + ex.getMessage());
        }
    }

    private void ensurePayConfigReady() {
        if (!payEnabled) {
            throw new BusinessException("微信支付尚未启用，请先配置 wechat.pay.enabled=true");
        }
        if (isBlank(appId) || isBlank(mchId) || isBlank(serialNo) || isBlank(privateKeyPath) || isBlank(apiV3Key) || isBlank(notifyUrl)) {
            throw new BusinessException("微信支付配置不完整，请检查 wechat.pay.*");
        }
    }

    private String normalizeChannel(String channel) {
        String value = channel == null ? "" : channel.trim().toUpperCase();
        if (!"JSAPI".equals(value) && !"NATIVE".equals(value)) {
            throw new BusinessException("不支持的支付渠道，仅支持 JSAPI / NATIVE");
        }
        return value;
    }

    private Order getOrderBelongingToUser(Long orderId, Long userId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException("订单不存在");
        }
        return order;
    }

    private String buildPaymentDescription(Long orderId) {
        OrderItem first = orderItemMapper.selectOne(
                new LambdaQueryWrapper<OrderItem>()
                        .eq(OrderItem::getOrderId, orderId)
                        .orderByAsc(OrderItem::getId)
                        .last("LIMIT 1")
        );
        if (first == null || first.getProductName() == null || first.getProductName().isBlank()) {
            return "环地福印刷商城订单";
        }
        String base = "环地福-" + first.getProductName();
        if (base.length() > 64) {
            return base.substring(0, 64);
        }
        return base;
    }

    private int toFen(BigDecimal yuan) {
        if (yuan == null || yuan.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("订单金额无效");
        }
        BigDecimal fen = yuan.multiply(new BigDecimal("100")).setScale(0, java.math.RoundingMode.HALF_UP);
        return fen.intValueExact();
    }

    private String buildOutTradeNo(Long orderId) {
        String seed = UUID.randomUUID().toString().replace("-", "");
        return "WOS" + System.currentTimeMillis() + orderId + seed.substring(0, 4);
    }

    private String randomNonce() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String resolveClientIp(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) {
            return "127.0.0.1";
        }
        return clientIp.trim();
    }

    private void markOrderPaid(Order order, String channel, String successTime) {
        if (order == null) {
            return;
        }
        if (!"PENDING".equals(order.getStatus())) {
            return;
        }
        order.setStatus("WAIT_PRODUCTION");
        order.setReviewStatus("PENDING");
        order.setReviewReason(null);
        order.setReviewedBy(null);
        order.setReviewedAt(null);
        if (channel != null && !channel.isBlank()) {
            order.setPayChannel(channel.trim().toUpperCase());
        }
        if (successTime != null && !successTime.isBlank()) {
            order.setPaidAt(parseWechatTime(successTime));
        } else if (order.getPaidAt() == null) {
            order.setPaidAt(LocalDateTime.now());
        }
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    private LocalDateTime parseWechatTime(String value) {
        try {
            return OffsetDateTime.parse(value).atZoneSameInstant(CN_ZONE).toLocalDateTime();
        } catch (DateTimeParseException ex) {
            return LocalDateTime.now();
        }
    }

    private String decryptNotifyResource(String ciphertext, String nonce, String associatedData) throws Exception {
        if (isBlank(ciphertext) || isBlank(nonce)) {
            throw new BusinessException("微信支付回调数据不完整");
        }
        byte[] keyBytes = apiV3Key.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length != 32) {
            throw new BusinessException("微信支付 APIv3 Key 长度必须为 32");
        }
        byte[] data = Base64.getDecoder().decode(ciphertext);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(128, nonce.getBytes(StandardCharsets.UTF_8));
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), spec);
        if (!isBlank(associatedData)) {
            cipher.updateAAD(associatedData.getBytes(StandardCharsets.UTF_8));
        }
        byte[] plain = cipher.doFinal(data);
        return new String(plain, StandardCharsets.UTF_8);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
