package com.gov.consumer.consumer;

import com.gov.consumer.model.GovNotification;
import com.gov.consumer.service.NotificationRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class GovNotificationConsumer {

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationRedisService redisService;

    @KafkaListener(
            topics = "gov-notifications",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(
            @Payload GovNotification notification,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("📥 [p:{} o:{}] id:{} | nationalId:{} | systemCode:{} | priority:{}",
                partition, offset,
                notification.getNotificationId(),
                notification.getNationalId(),
                notification.getSystemCode(),
                notification.getPriority());

        // 1. تسجيل وقت الاستلام
        notification.setDeliveredAt(LocalDateTime.now());
        notification.setDeliveryStatus("DELIVERED");

        // 2. حفظ في Redis
        redisService.save(notification);

        // 3. التوجيه عبر WebSocket
        route(notification);
    }

    /**
     * منطق التوجيه الكامل بناءً على nationalId و systemCode
     *
     * الحالة 1: nationalId + systemCode → مستخدم محدد في نظام محدد
     *           topic: /topic/{SYSTEM_CODE}/{nationalId}
     *
     * الحالة 2: systemCode فقط → كل مستخدمي نظام محدد
     *           topic: /topic/{SYSTEM_CODE}
     *
     * الحالة 3: nationalId فقط → مستخدم محدد في كل الأنظمة
     *           topic: /topic/user/{nationalId}
     *
     * الحالة 4: لا شيء → broadcast عام لكل الأنظمة
     *           topic: /topic/notifications
     */
    private void route(GovNotification n) {
        boolean hasUser   = n.getNationalId() != null && !n.getNationalId().isBlank();
        boolean hasSystem = n.getSystemCode() != null && !n.getSystemCode().isBlank();

        if (hasUser && hasSystem) {
            // مستخدم محدد في نظام محدد
            String wsTopic = "/topic/" + n.getSystemCode() + "/" + n.getNationalId();
            messagingTemplate.convertAndSend(wsTopic, n);
            log.info("📡 → [{}] in [{}]", n.getNationalId(), n.getSystemCode());

        } else if (hasSystem) {
            // broadcast لكل مستخدمي النظام
            String wsTopic = "/topic/" + n.getSystemCode();
            messagingTemplate.convertAndSend(wsTopic, n);
            log.info("📡 → All users in [{}]", n.getSystemCode());

        } else if (hasUser) {
            // مستخدم محدد في كل الأنظمة
            String wsTopic = "/topic/user/" + n.getNationalId();
            messagingTemplate.convertAndSend(wsTopic, n);
            log.info("📡 → User [{}] all systems", n.getNationalId());

        } else {
            // broadcast عام
            messagingTemplate.convertAndSend("/topic/notifications", n);
            log.info("📡 → Global broadcast");
        }
    }
}
