package com.notification.api.service;

import com.notification.api.common.BusinessException;
import com.notification.api.common.NotificationResponseCode;
import com.notification.api.domain.Notification;
import com.notification.api.domain.NotificationMessage;
import com.notification.api.domain.OutboxEvent;
import com.notification.api.dto.NotificationDispatchResponse;
import com.notification.api.event.NotificationSendEvent;
import com.notification.api.repository.NotificationMessageRepository;
import com.notification.api.repository.NotificationRepository;
import com.notification.api.repository.OutboxEventRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * 접수된 발송 요청을 발송 대기열에 올립니다.
 *
 * <p>v1은 이 자리에서 수신자를 하나씩 발송사로 보냈고, 그래서 10만 건에 6시간 19분이 걸렸습니다.
 * v2는 발송사를 호출하지 않습니다. 수신자별 발송 지시를 outbox에 쌓고 바로 응답하며,
 * 실제 발송은 워커가 Kafka에서 이어받아 처리합니다.
 *
 * <p>상태 변경과 outbox 적재를 하나의 트랜잭션으로 묶는 것이 핵심입니다. 둘 중 하나만
 * 저장되면 "발송 시작했다고 기록됐는데 발송 지시는 어디에도 없는" 상태가 되어 조용히 유실됩니다.
 * 발송 전체를 한 트랜잭션에 묶지 않았던 v1과는 반대 방향의 선택인데, 여기서 묶는 대상은
 * 발송 자체가 아니라 접수 기록이므로 오래 걸리지 않습니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationDispatchService {

	private final NotificationRepository notificationRepository;
	private final NotificationMessageRepository messageRepository;
	private final OutboxEventRepository outboxRepository;
	private final ObjectMapper objectMapper;

	/**
	 * 발송 요청에 속한 모든 수신자의 발송 지시를 outbox에 쌓습니다.
	 *
	 * <p>반환 시점에는 아직 아무것도 발송되지 않았습니다. 소요시간도 발송이 아니라
	 * 접수에 걸린 시간입니다.
	 *
	 * @param notificationId 발송 요청 식별자
	 * @return outbox에 쌓은 건수와 접수 소요시간
	 * @throws BusinessException 발송 요청이 없거나 이미 발송을 시작한 경우
	 */
	@Transactional
	public NotificationDispatchResponse dispatch(Long notificationId) {
		Notification notification = notificationRepository.findById(notificationId)
				.orElseThrow(() -> new BusinessException(
						NotificationResponseCode.NOTIFICATION_NOT_FOUND_FAIL, HttpStatus.NOT_FOUND));

		if (!notification.isDispatchable()) {
			throw new BusinessException(
					NotificationResponseCode.NOTIFICATION_ALREADY_DISPATCHED_FAIL, HttpStatus.CONFLICT);
		}

		LocalDateTime startedAt = LocalDateTime.now();
		notification.startDispatch(startedAt);
		notificationRepository.save(notification);

		List<NotificationMessage> messages = messageRepository.findAllByNotificationId(notificationId);
		for (NotificationMessage message : messages) {
			outboxRepository.save(toOutboxEvent(notification, message));
		}

		long elapsedMillis = Duration.between(startedAt, LocalDateTime.now()).toMillis();
		log.info("[dispatch] 발송 지시 적재 완료: notificationId={}, queued={}, elapsedMillis={}",
				notificationId, messages.size(), elapsedMillis);

		return new NotificationDispatchResponse(notificationId, messages.size(), elapsedMillis);
	}

	private OutboxEvent toOutboxEvent(Notification notification, NotificationMessage message) {
		NotificationSendEvent event = NotificationSendEvent.create(notification, message);
		return OutboxEvent.create(
				event.eventId(), event.notificationId(), event.messageId(), serialize(event));
	}

	/**
	 * 이벤트를 JSON 문자열로 직렬화합니다.
	 *
	 * <p>직렬화에 실패하면 트랜잭션을 되돌립니다. 일부만 쌓인 채로 커밋되면 나머지 수신자가
	 * 발송되지 않은 것을 아무도 알아채지 못하기 때문입니다.
	 */
	private String serialize(NotificationSendEvent event) {
		try {
			return objectMapper.writeValueAsString(event);
		} catch (JacksonException e) {
			throw new IllegalStateException(
					"발송 이벤트 직렬화에 실패했습니다. messageId=" + event.messageId(), e);
		}
	}
}
