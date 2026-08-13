package com.notification.worker.repository;

import com.notification.worker.domain.NotificationMessage;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 발송 건을 조회하고 발송 결과를 저장합니다.
 */
public interface NotificationMessageRepository extends JpaRepository<NotificationMessage, Long> {
}
