package com.wonderful.onlineshop.operationlog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wonderful.onlineshop.operationlog.entity.AdminOperationLog;
import com.wonderful.onlineshop.operationlog.mapper.AdminOperationLogMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AdminOperationLogService {
    private final AdminOperationLogMapper mapper;

    public AdminOperationLogService(AdminOperationLogMapper mapper) {
        this.mapper = mapper;
    }

    public void save(AdminOperationLog log) {
        mapper.insert(log);
    }

    public IPage<AdminOperationLog> page(int page, int size, Long adminUserId, String moduleName, String actionName,
                                         String keyword, LocalDateTime from, LocalDateTime to) {
        LambdaQueryWrapper<AdminOperationLog> wrapper = new LambdaQueryWrapper<>();
        if (adminUserId != null && adminUserId > 0) wrapper.eq(AdminOperationLog::getAdminUserId, adminUserId);
        if (moduleName != null && !moduleName.isBlank()) wrapper.eq(AdminOperationLog::getModuleName, moduleName.trim());
        if (actionName != null && !actionName.isBlank()) wrapper.eq(AdminOperationLog::getActionName, actionName.trim());
        if (keyword != null && !keyword.isBlank()) {
            String key = keyword.trim();
            wrapper.and(w -> w.like(AdminOperationLog::getAdminNickname, key)
                    .or().like(AdminOperationLog::getAdminCompanyName, key)
                    .or().like(AdminOperationLog::getRequestPath, key)
                    .or().like(AdminOperationLog::getSummary, key));
        }
        if (from != null) wrapper.ge(AdminOperationLog::getCreatedAt, from);
        if (to != null) wrapper.le(AdminOperationLog::getCreatedAt, to);
        wrapper.orderByDesc(AdminOperationLog::getCreatedAt);
        return mapper.selectPage(new Page<>(page, size), wrapper);
    }

    public int deleteOlderThan(LocalDateTime cutoffTime) {
        if (cutoffTime == null) return 0;
        LambdaQueryWrapper<AdminOperationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(AdminOperationLog::getCreatedAt, cutoffTime);
        return mapper.delete(wrapper);
    }
}
