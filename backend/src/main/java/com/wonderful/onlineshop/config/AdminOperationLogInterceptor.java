package com.wonderful.onlineshop.config;

import cn.dev33.satoken.stp.StpUtil;
import com.wonderful.onlineshop.operationlog.entity.AdminOperationLog;
import com.wonderful.onlineshop.operationlog.service.AdminOperationLogService;
import com.wonderful.onlineshop.user.entity.User;
import com.wonderful.onlineshop.user.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class AdminOperationLogInterceptor implements HandlerInterceptor {
    private static final Pattern LAST_NUMBER_PATTERN = Pattern.compile("/(\\d+)(?:/[^/]*)?$");
    private final AdminOperationLogService operationLogService;
    private final UserMapper userMapper;

    public AdminOperationLogInterceptor(AdminOperationLogService operationLogService, UserMapper userMapper) {
        this.operationLogService = operationLogService;
        this.userMapper = userMapper;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (!StpUtil.isLogin()) return;
        String uri = request.getRequestURI();
        if (uri == null || !uri.startsWith("/admin/")) return;
        if (uri.startsWith("/admin/operation-logs")) return;
        try {
            Long adminUserId = StpUtil.getLoginIdAsLong();
            User user = userMapper.selectById(adminUserId);
            AdminOperationLog log = new AdminOperationLog();
            log.setAdminUserId(adminUserId);
            log.setAdminNickname(user == null ? null : user.getNickname());
            log.setAdminCompanyName(user == null ? null : user.getCompanyName());
            log.setHttpMethod(request.getMethod());
            log.setRequestPath(uri);
            log.setModuleName(parseModule(uri));
            log.setActionName(parseAction(request.getMethod()));
            log.setTargetId(parseTargetId(uri));
            log.setSummary(buildSummary(request));
            log.setIpAddress(getClientIp(request));
            log.setUserAgent(trim(request.getHeader("User-Agent"), 300));
            log.setStatusCode(response.getStatus());
            log.setCreatedAt(LocalDateTime.now());
            operationLogService.save(log);
        } catch (Exception ignored) {
        }
    }

    private String parseModule(String uri) {
        String[] parts = uri.split("/");
        return parts.length > 2 ? parts[2] : "unknown";
    }

    private String parseAction(String method) {
        if (method == null) return "UNKNOWN";
        return switch (method.toUpperCase()) {
            case "POST" -> "CREATE";
            case "PUT", "PATCH" -> "UPDATE";
            case "DELETE" -> "DELETE";
            case "GET" -> "QUERY";
            default -> method.toUpperCase();
        };
    }

    private Long parseTargetId(String uri) {
        Matcher matcher = LAST_NUMBER_PATTERN.matcher(uri);
        if (!matcher.find()) return null;
        try {
            return Long.parseLong(matcher.group(1));
        } catch (Exception ex) {
            return null;
        }
    }

    private String buildSummary(HttpServletRequest request) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String qs = request.getQueryString();
        String text = method + " " + uri + (qs == null || qs.isBlank() ? "" : ("?" + qs));
        return trim(text, 500);
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int idx = xff.indexOf(',');
            return idx > 0 ? xff.substring(0, idx).trim() : xff.trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) return realIp.trim();
        return request.getRemoteAddr();
    }

    private String trim(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
