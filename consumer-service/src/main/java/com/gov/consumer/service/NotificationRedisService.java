package com.gov.consumer.service;

import com.gov.consumer.model.GovNotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationRedisService {

    private final RedisTemplate<String, GovNotification> redisTemplate;

    private static final String GLOBAL_KEY   = "gov:notif:global";
    private static final String USER_PREFIX  = "gov:notif:user:";
    private static final String SYS_PREFIX   = "gov:notif:system:";
    private static final String DLQ_KEY      = "gov:notif:failed";
    private static final long   TTL_DAYS     = 30;
    private static final long   MAX_SIZE     = 500;

    public void save(GovNotification n) {
        // حفظ حسب نوع الإشعار
        if (n.getNationalId() != null && n.getSystemCode() != null) {
            // مستخدم + نظام
            push(USER_PREFIX + n.getSystemCode() + ":" + n.getNationalId(), n);
        } else if (n.getNationalId() != null) {
            // مستخدم فقط
            push(USER_PREFIX + n.getNationalId(), n);
        } else if (n.getSystemCode() != null) {
            // نظام فقط
            push(SYS_PREFIX + n.getSystemCode(), n);
        } else {
            // عام
            push(GLOBAL_KEY, n);
        }
    }

    public void saveToDeadLetter(GovNotification n) {
        push(DLQ_KEY, n);
        log.error("💀 Saved to DLQ | id:{}", n.getNotificationId());
    }

    // جلب كل إشعارات مستخدم في نظام معين
    public List<GovNotification> getUserSystemNotifications(String nationalId, String systemCode) {
        List<GovNotification> result = new ArrayList<>();
        result.addAll(getList(GLOBAL_KEY));
        result.addAll(getList(SYS_PREFIX + systemCode));
        result.addAll(getList(USER_PREFIX + nationalId));
        result.addAll(getList(USER_PREFIX + systemCode + ":" + nationalId));
        sortByDate(result);
        return result;
    }

    // جلب كل إشعارات مستخدم
    public List<GovNotification> getUserNotifications(String nationalId) {
        List<GovNotification> result = new ArrayList<>();
        result.addAll(getList(GLOBAL_KEY));
        result.addAll(getList(USER_PREFIX + nationalId));
        sortByDate(result);
        return result;
    }

    // جلب إشعارات نظام
    public List<GovNotification> getSystemNotifications(String systemCode) {
        List<GovNotification> result = new ArrayList<>();
        result.addAll(getList(GLOBAL_KEY));
        result.addAll(getList(SYS_PREFIX + systemCode));
        sortByDate(result);
        return result;
    }

    public List<GovNotification> getFailedNotifications() {
        return getList(DLQ_KEY);
    }

    private void push(String key, GovNotification n) {
        redisTemplate.opsForList().leftPush(key, n);
        redisTemplate.opsForList().trim(key, 0, MAX_SIZE - 1);
        redisTemplate.expire(key, Duration.ofDays(TTL_DAYS));
        log.info("💾 Saved | key:{} | id:{}", key, n.getNotificationId());
    }

    private List<GovNotification> getList(String key) {
        List<GovNotification> r = redisTemplate.opsForList().range(key, 0, -1);
        return r != null ? r : new ArrayList<>();
    }

    private void sortByDate(List<GovNotification> list) {
        list.sort((a, b) -> {
            if (a.getCreatedAt() == null) return 1;
            if (b.getCreatedAt() == null) return -1;
            return b.getCreatedAt().compareTo(a.getCreatedAt());
        });
    }
}
