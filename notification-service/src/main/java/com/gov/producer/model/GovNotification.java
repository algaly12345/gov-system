package com.gov.producer.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GovNotification {

    @Builder.Default
    private String notificationId = UUID.randomUUID().toString();

    @NotBlank(message = "العنوان مطلوب")
    private String title;

    @NotBlank(message = "المحتوى مطلوب")
    private String message;

    @NotNull(message = "النوع مطلوب")
    private NotificationType type;

    // رقم الهوية الوطنية — null يعني broadcast للكل
    private String nationalId;

    // كود النظام / التطبيق المستهدف
    // مثال: ABSHER, HEALTH_APP, MOI_APP, SALARY_APP
    // null يعني يصل لكل الأنظمة
    private String systemCode;

    // اسم النظام للعرض في الواجهة
    private String systemName;

    // رقم المعاملة الحكومية
    private String transactionId;

    // الجهة الحكومية المرسلة
    private String senderEntity;

    @Builder.Default
    private Priority priority = Priority.NORMAL;

    @Builder.Default
    private DeliveryStatus deliveryStatus = DeliveryStatus.PENDING;

    @Builder.Default
    private int retryCount = 0;

    @Builder.Default
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt = LocalDateTime.now();

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime deliveredAt;

    public enum NotificationType {
        INFO, SUCCESS, WARNING, ERROR, URGENT, SYSTEM
    }

    public enum Priority {
        LOW, NORMAL, HIGH, CRITICAL
    }

    public enum DeliveryStatus {
        PENDING, SENT, DELIVERED, FAILED, RETRYING
    }
}
