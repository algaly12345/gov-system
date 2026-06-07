package com.gov.producer.service;

import com.gov.producer.model.GovNotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationProducerService {

    private static final String TOPIC = "gov-notifications";
    private final KafkaTemplate<String, GovNotification> kafkaTemplate;

    public void send(GovNotification notification) {
        // key يحدد الـ partition — نفس المستخدم دائماً على نفس الـ partition (ترتيب مضمون)
        String key = buildKey(notification);

        log.info("📤 Sending | id:{} | nationalId:{} | systemCode:{} | priority:{} | key:{}",
                notification.getNotificationId(),
                notification.getNationalId(),
                notification.getSystemCode(),
                notification.getPriority(),
                key);

        kafkaTemplate.send(TOPIC, key, notification)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        notification.setDeliveryStatus(GovNotification.DeliveryStatus.SENT);
                        log.info("✅ Sent | id:{} | partition:{} | offset:{}",
                                notification.getNotificationId(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    } else {
                        notification.setDeliveryStatus(GovNotification.DeliveryStatus.FAILED);
                        log.error("❌ Failed | id:{} | error:{}", notification.getNotificationId(), ex.getMessage());
                    }
                });
    }

    private String buildKey(GovNotification n) {
        if (n.getNationalId() != null && n.getSystemCode() != null)
            return n.getSystemCode() + ":" + n.getNationalId();
        if (n.getNationalId() != null)
            return n.getNationalId();
        if (n.getSystemCode() != null)
            return n.getSystemCode();
        return "broadcast";
    }
}
