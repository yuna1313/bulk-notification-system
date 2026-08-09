package com.notification.api.repository;

import com.notification.api.domain.NotificationMessage;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 발송 건을 조회하고 저장합니다.
 */
public interface NotificationMessageRepository extends JpaRepository<NotificationMessage, Long> {
}
