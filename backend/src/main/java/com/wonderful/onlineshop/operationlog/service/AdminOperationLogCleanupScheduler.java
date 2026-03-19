package com.wonderful.onlineshop.operationlog.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class AdminOperationLogCleanupScheduler {
    private static final Logger log = LoggerFactory.getLogger(AdminOperationLogCleanupScheduler.class);

    @Value("${app.operation-log-cleanup.enabled:true}")
    private boolean enabled;

    @Value("${app.operation-log-cleanup.retain-days:180}")
    private int retainDays;

    @Value("${app.operation-log-cleanup.dry-run:false}")
    private boolean dryRun;

    private final AdminOperationLogService operationLogService;

    public AdminOperationLogCleanupScheduler(AdminOperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @Scheduled(cron = "${app.operation-log-cleanup.cron:0 45 3 * * ?}")
    public void cleanup() {
        if (!enabled) {
            return;
        }
        int safeRetainDays = Math.max(1, retainDays);
        LocalDateTime cutoff = LocalDateTime.now().minusDays(safeRetainDays);
        if (dryRun) {
            log.info("[dry-run] operation-log cleanup skipped delete, cutoff={}, retainDays={}", cutoff, safeRetainDays);
            return;
        }
        int deleted = operationLogService.deleteOlderThan(cutoff);
        log.info("Operation-log cleanup finished: retainDays={}, cutoff={}, deleted={}", safeRetainDays, cutoff, deleted);
    }
}
