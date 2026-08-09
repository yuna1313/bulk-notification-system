package com.notification.api.repository;

import com.notification.api.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 발송 요청을 조회하고 저장합니다.
 */
public interface NotificationRepository extends JpaRepository<Notification, Long> {
}
