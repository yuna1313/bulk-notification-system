package com.notification.api.service;

import com.notification.api.client.MockProviderClient;
import com.notification.api.client.ProviderSendResult;
import com.notification.api.client.dto.ProviderSendRequest;
import com.notification.api.common.BusinessException;
import com.notification.api.common.NotificationResponseCode;
import com.notification.api.domain.Notification;
import com.notification.api.domain.NotificationMessage;
import com.notification.api.dto.NotificationDispatchResponse;
import com.notification.api.repository.NotificationMessageRepository;
import com.notification.api.repository.NotificationRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * 접수된 발송 요청을 외부 발송사로 실제 발송합니다.
 *
 * <p>v1은 성능 한계를 측정하는 것이 목적이므로 수신자를 for 문으로 하나씩 동기 호출하고
 * 결과를 건건이 DB에 반영합니다. 비동기 처리, 스레드풀, 재시도, 배치를 넣지 않습니다.
 *
 * <p>발송 전체를 하나의 트랜잭션으로 묶지 않습니다. 10만 건을 한 트랜잭션에 묶으면
 * 발송이 끝날 때까지 결과가 보이지 않아 발송 현황 조회가 의미를 잃기 때문입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationDispatchService {

	private final NotificationRepository notificationRepository;
	private final NotificationMessageRepository messageRepository;
	private final MockProviderClient providerClient;

	/**
	 * 발송 요청에 속한 모든 수신자에게 순차적으로 발송합니다.
	 *
	 * @param notificationId 발송 요청 식별자
	 * @return 성공 건수, 실패 건수, 소요시간이 담긴 발송 결과
	 * @throws BusinessException 발송 요청이 없거나 이미 발송을 시작한 경우
	 */
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
		int successCount = 0;
		for (NotificationMessage message : messages) {
			if (sendOne(notification, message)) {
				successCount++;
			}
		}

		LocalDateTime finishedAt = LocalDateTime.now();
		notification.finishDispatch(finishedAt);
		notificationRepository.save(notification);

		long elapsedMillis = Duration.between(startedAt, finishedAt).toMillis();
		int failCount = messages.size() - successCount;
		log.info("[dispatch] 발송 완료: notificationId={}, total={}, success={}, fail={}, elapsedMillis={}",
				notificationId, messages.size(), successCount, failCount, elapsedMillis);

		return new NotificationDispatchResponse(
				notificationId, messages.size(), successCount, failCount, elapsedMillis);
	}

	private boolean sendOne(Notification notification, NotificationMessage message) {
		ProviderSendResult result = providerClient.send(new ProviderSendRequest(
				String.valueOf(message.getId()),
				message.getRecipientId(),
				notification.getChannel().name(),
				notification.getContent()
		));

		LocalDateTime sentAt = LocalDateTime.now();
		if (result.success()) {
			message.markSuccess(result.providerMessageId(), sentAt);
		} else {
			message.markFail(result.failureReason(), sentAt);
		}
		messageRepository.save(message);

		return result.success();
	}
}
