package com.wonderful.onlineshop.controller;

import com.wonderful.onlineshop.order.service.OrderPaymentService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/pay/wechat")
public class WechatPayNotifyController {

    private final OrderPaymentService orderPaymentService;

    public WechatPayNotifyController(OrderPaymentService orderPaymentService) {
        this.orderPaymentService = orderPaymentService;
    }

    @PostMapping(value = "/notify", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> notify(@RequestBody(required = false) String body) {
        boolean success = orderPaymentService.handleWechatNotify(body);
        Map<String, String> response = new HashMap<>();
        if (success) {
            response.put("code", "SUCCESS");
            response.put("message", "成功");
        } else {
            response.put("code", "FAIL");
            response.put("message", "处理失败");
        }
        return response;
    }
}
