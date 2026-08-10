package com.notification.api.service;

import com.notification.api.domain.Notification;
import com.notification.api.dto.NotificationCreateRequest;
import com.notification.api.dto.NotificationCreateResponse;
import com.notification.api.dto.TestNotificationCreateRequest;
import com.notification.api.repository.NotificationMessageRepository;
import com.notification.api.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 발송 요청을 접수해 수신자별 발송 건까지 저장합니다.
 *
 * <p>v1은 성능 한계를 측정하는 것이 목적이므로 수신자를 한 건씩 저장합니다.
 * 벌크 insert나 배치 처리를 넣지 않습니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

	private static final String TEST_RECIPIENT_ID_PREFIX = "user-";

	private final NotificationRepository notificationRepository;
	private final NotificationMessageRepository messageRepository;

	/**
	 * 수신자 목록을 받아 발송 요청을 접수합니다.
	 *
	 * @param request 제목, 내용, 발송 수단, 예약 시각, 수신자 목록
	 * @return 접수된 발송 요청 식별자와 수신자 수
	 */
	@Transactional
	public NotificationCreateResponse create(NotificationCreateRequest request) {
		Notification notification = notificationRepository.save(Notification.create(
				request.getTitle(),
				request.getContent(),
				request.getChannel(),
				request.getScheduledAt()
		));

		for (String recipientId : request.getRecipientIds()) {
			messageRepository.save(notification.addMessage(recipientId));
		}

		int recipientCount = request.getRecipientIds().size();
		log.info("[create] 발송 요청 접수: notificationId={}, recipientCount={}", notification.getId(), recipientCount);
		return new NotificationCreateResponse(notification.getId(), recipientCount);
	}

	/**
	 * 부하 테스트를 위해 수신자 수만 받아 발송 요청을 접수합니다.
	 *
	 * <p>수신자 식별자는 user-1부터 user-N까지 순번으로 만듭니다.
	 *
	 * @param request 제목, 내용, 발송 수단, 예약 시각, 수신자 수
	 * @return 접수된 발송 요청 식별자와 수신자 수
	 */
	@Transactional
	public NotificationCreateResponse createWithGeneratedRecipients(TestNotificationCreateRequest request) {
		Notification notification = notificationRepository.save(Notification.create(
				request.getTitle(),
				request.getContent(),
				request.getChannel(),
				request.getScheduledAt()
		));

		int recipientCount = request.getRecipientCount();
		for (int sequence = 1; sequence <= recipientCount; sequence++) {
			messageRepository.save(notification.addMessage(TEST_RECIPIENT_ID_PREFIX + sequence));
		}

		log.info("[createWithGeneratedRecipients] 테스트용 발송 요청 접수: notificationId={}, recipientCount={}",
				notification.getId(), recipientCount);
		return new NotificationCreateResponse(notification.getId(), recipientCount);
	}
}
