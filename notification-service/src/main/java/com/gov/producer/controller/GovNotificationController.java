package com.gov.producer.controller;

import com.gov.producer.model.GovNotification;
import com.gov.producer.service.NotificationProducerService;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class GovNotificationController {

    private final NotificationProducerService producerService;

    // ─────────────────────────────────────────────────────────────────────────
    // 1. إشعار عام لكل الأنظمة وكل المستخدمين
    //    POST /api/v1/notifications/send
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendGlobal(
            @Valid @RequestBody GovNotification notification) {

        producerService.send(notification);
        return buildResponse(notification, "إشعار عام لجميع الأنظمة");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. إشعار لمستخدم محدد (بالهوية) — يصل لكل أجهزته في كل الأنظمة
    //    POST /api/v1/notifications/send/user/{nationalId}
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/send/user/{nationalId}")
    public ResponseEntity<Map<String, Object>> sendToUser(
            @PathVariable String nationalId,
            @Valid @RequestBody GovNotification notification) {

        notification.setNationalId(nationalId);
        producerService.send(notification);
        return buildResponse(notification, "إشعار للمستخدم: " + nationalId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. إشعار لكل مستخدمي نظام/تطبيق محدد (broadcast للتطبيق)
    //    POST /api/v1/notifications/send/system/{systemCode}
    //
    //    مثال: POST /api/v1/notifications/send/system/ABSHER
    //    → يصل لكل المتصلين بتطبيق ABSHER
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/send/system/{systemCode}")
    public ResponseEntity<Map<String, Object>> sendToSystem(
            @PathVariable String systemCode,
            @Valid @RequestBody GovNotification notification) {

        notification.setSystemCode(systemCode.toUpperCase());
        producerService.send(notification);
        return buildResponse(notification, "إشعار لكل مستخدمي نظام: " + systemCode);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. إشعار لمستخدم محدد في نظام محدد ← الأهم
    //    POST /api/v1/notifications/send/user/{nationalId}/system/{systemCode}
    //
    //    مثال: POST /api/v1/notifications/send/user/1234567890/system/HEALTH_APP
    //    → يصل لـ 1234567890 فقط في تطبيق HEALTH_APP
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/send/user/{nationalId}/system/{systemCode}")
    public ResponseEntity<Map<String, Object>> sendToUserInSystem(
            @PathVariable String nationalId,
            @PathVariable String systemCode,
            @Valid @RequestBody GovNotification notification) {

        notification.setNationalId(nationalId);
        notification.setSystemCode(systemCode.toUpperCase());
        producerService.send(notification);
        return buildResponse(notification,
                "إشعار للمستخدم " + nationalId + " في نظام " + systemCode);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. إرسال جماعي لقائمة مستخدمين في نظام محدد
    //    POST /api/v1/notifications/send/bulk/system/{systemCode}
    //
    //    مثال: POST /api/v1/notifications/send/bulk/system/MOI_APP
    //    مع body يحتوي قائمة الهويات
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/send/bulk/system/{systemCode}")
    public ResponseEntity<Map<String, Object>> sendBulkToSystem(
            @PathVariable String systemCode,
            @RequestBody BulkRequest request) {

        int count = 0;
        for (String nationalId : request.getNationalIds()) {
            GovNotification n = GovNotification.builder()
                    .title(request.getTitle())
                    .message(request.getMessage())
                    .type(request.getType())
                    .nationalId(nationalId)
                    .systemCode(systemCode.toUpperCase())
                    .systemName(request.getSystemName())
                    .senderEntity(request.getSenderEntity())
                    .priority(request.getPriority() != null
                            ? request.getPriority() : GovNotification.Priority.NORMAL)
                    .build();
            producerService.send(n);
            count++;
        }

        log.info("📢 Bulk | system:{} | count:{}", systemCode, count);
        return ResponseEntity.ok(Map.of(
                "status", "queued",
                "systemCode", systemCode.toUpperCase(),
                "totalQueued", count,
                "message", "تم إرسال " + count + " إشعار لنظام " + systemCode
        ));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 6. إشعار عاجل CRITICAL لمستخدم في نظام محدد
    //    POST /api/v1/notifications/urgent/user/{nationalId}/system/{systemCode}
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/urgent/user/{nationalId}/system/{systemCode}")
    public ResponseEntity<Map<String, Object>> sendUrgent(
            @PathVariable String nationalId,
            @PathVariable String systemCode,
            @Valid @RequestBody GovNotification notification) {

        notification.setNationalId(nationalId);
        notification.setSystemCode(systemCode.toUpperCase());
        notification.setPriority(GovNotification.Priority.CRITICAL);
        notification.setType(GovNotification.NotificationType.URGENT);
        producerService.send(notification);
        return buildResponse(notification, "إشعار عاجل → " + nationalId + " في " + systemCode);
    }

    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("service", "notification-service", "status", "UP"));
    }

    private ResponseEntity<Map<String, Object>> buildResponse(GovNotification n, String msg) {
        return ResponseEntity.ok(Map.of(
                "status", "queued",
                "notificationId", n.getNotificationId(),
                "nationalId",  n.getNationalId()  != null ? n.getNationalId()  : "ALL",
                "systemCode",  n.getSystemCode()  != null ? n.getSystemCode()  : "ALL",
                "systemName",  n.getSystemName()  != null ? n.getSystemName()  : "-",
                "priority",    n.getPriority().name(),
                "message",     msg
        ));
    }

    @Data
    public static class BulkRequest {
        private List<String> nationalIds;
        private String title;
        private String message;
        private GovNotification.NotificationType type;
        private String senderEntity;
        private String systemName;
        private GovNotification.Priority priority;
    }
}
