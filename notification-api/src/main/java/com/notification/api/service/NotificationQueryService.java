package com.notification.api.service;

import com.notification.api.common.BusinessException;
import com.notification.api.common.NotificationResponseCode;
import com.notification.api.domain.MessageStatus;
import com.notification.api.domain.Notification;
import com.notification.api.dto.NotificationStatusResponse;
import com.notification.api.repository.MessageStatusCount;
import com.notification.api.repository.NotificationMessageRepository;
import com.notification.api.repository.NotificationRepository;
import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 발송 요청의 진행 현황을 조회합니다.
 *
 * <p>성공, 실패 건수를 별도 컬럼에 누적해두지 않고 조회 시점마다 집계합니다.
 * 발송 건이 많아질수록 이 조회가 느려지는 것 자체가 v1에서 측정하려는 대상입니다.
 */
@Service
@RequiredArgsConstructor
public class NotificationQueryService {

	private final NotificationRepository notificationRepository;
	private final NotificationMessageRepository messageRepository;

	/**
	 * 발송 요청의 상태와 상태별 건수를 조회합니다.
	 *
	 * @param notificationId 발송 요청 식별자
	 * @return 발송 요청 정보와 성공, 실패, 대기 건수
	 * @throws BusinessException 발송 요청이 없는 경우
	 */
	@Transactional(readOnly = true)
	public NotificationStatusResponse getStatus(Long notificationId) {
		Notification notification = notificationRepository.findById(notificationId)
				.orElseThrow(() -> new BusinessException(
						NotificationResponseCode.NOTIFICATION_NOT_FOUND_FAIL, HttpStatus.NOT_FOUND));

		Map<MessageStatus, Long> counts = new EnumMap<>(MessageStatus.class);
		for (MessageStatusCount statusCount : messageRepository.countGroupByStatus(notificationId)) {
			counts.put(statusCount.getStatus(), statusCount.getCount());
		}

		long successCount = counts.getOrDefault(MessageStatus.SUCCESS, 0L);
		long failCount = counts.getOrDefault(MessageStatus.FAIL, 0L);
		long pendingCount = counts.getOrDefault(MessageStatus.PENDING, 0L);

		return new NotificationStatusResponse(
				notification.getId(),
				notification.getTitle(),
				notification.getChannel(),
				notification.getStatus(),
				notification.getScheduledAt(),
				notification.getDispatchStartedAt(),
				notification.getDispatchFinishedAt(),
				elapsedMillis(notification),
				successCount + failCount + pendingCount,
				successCount,
				failCount,
				pendingCount
		);
	}

	private Long elapsedMillis(Notification notification) {
		if (notification.getDispatchStartedAt() == null || notification.getDispatchFinishedAt() == null) {
			return null;
		}
		return Duration.between(notification.getDispatchStartedAt(), notification.getDispatchFinishedAt())
				.toMillis();
	}
}
