package com.gov.consumer.controller;

import com.gov.consumer.model.GovNotification;
import com.gov.consumer.service.NotificationRedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class NotificationQueryController {

    private final NotificationRedisService redisService;

    // جلب إشعارات مستخدم في نظام محدد
    @GetMapping("/user/{nationalId}/system/{systemCode}")
    public ResponseEntity<List<GovNotification>> getUserSystemNotifications(
            @PathVariable String nationalId,
            @PathVariable String systemCode) {
        return ResponseEntity.ok(
                redisService.getUserSystemNotifications(nationalId, systemCode.toUpperCase()));
    }

    // جلب كل إشعارات مستخدم
    @GetMapping("/user/{nationalId}")
    public ResponseEntity<List<GovNotification>> getUserNotifications(
            @PathVariable String nationalId) {
        return ResponseEntity.ok(redisService.getUserNotifications(nationalId));
    }

    // جلب إشعارات نظام
    @GetMapping("/system/{systemCode}")
    public ResponseEntity<List<GovNotification>> getSystemNotifications(
            @PathVariable String systemCode) {
        return ResponseEntity.ok(redisService.getSystemNotifications(systemCode.toUpperCase()));
    }

    // جلب الإشعارات الفاشلة (Dead Letter)
    @GetMapping("/admin/failed")
    public ResponseEntity<List<GovNotification>> getFailedNotifications() {
        return ResponseEntity.ok(redisService.getFailedNotifications());
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "service", "consumer-service",
                "status", "UP",
                "redis", "connected",
                "websocket", "active"
        ));
    }
}
