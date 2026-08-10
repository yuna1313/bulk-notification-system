package com.notification.worker.service;

import com.notification.worker.client.MockProviderClient;
import com.notification.worker.client.ProviderSendResult;
import com.notification.worker.client.dto.ProviderSendRequest;
import com.notification.worker.domain.NotificationMessage;
import com.notification.worker.event.NotificationSendEvent;
import com.notification.worker.repository.NotificationMessageRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 발송 지시 한 건을 실제로 발송하고 결과를 기록합니다.
 *
 * <p>v1에서 notification-api가 for 문 안에서 하던 일이 그대로 여기로 옮겨왔습니다.
 * 달라진 것은 순서대로 한 번에 도는 대신, 컨슈머 스레드 여러 개가 나눠서 돈다는 점뿐입니다.
 *
 * <p>메서드에 트랜잭션을 걸지 않았습니다. 걸면 발송사 응답을 기다리는 200ms 내내 DB 커넥션을
 * 붙잡게 되고, 컨슈머를 늘릴수록 커넥션 풀이 먼저 말라붙습니다. 대신 조회와 저장이 각각
 * 짧은 트랜잭션으로 끊기고, 그 사이의 발송사 호출은 트랜잭션 밖에서 일어납니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationSendService {

	private final MockProviderClient providerClient;
	private final NotificationMessageRepository messageRepository;

	/**
	 * 발송 지시를 받아 발송사를 호출하고 결과를 발송 건에 기록합니다.
	 *
	 * <p>같은 지시가 두 번 배달되면 발송도 두 번 나갑니다. 중복을 걸러내는 처리는
	 * 아직 없으며 다음 단계에서 붙입니다.
	 *
	 * @param event 발송 지시
	 */
	public void send(NotificationSendEvent event) {
		Optional<NotificationMessage> found = messageRepository.findById(event.messageId());
		if (found.isEmpty()) {
			log.warn("[send] 발송 건을 찾지 못해 건너뜁니다. messageId={}, eventId={}",
					event.messageId(), event.eventId());
			return;
		}

		ProviderSendResult result = providerClient.send(new ProviderSendRequest(
				String.valueOf(event.messageId()),
				event.recipientId(),
				event.channel(),
				event.content()
		));

		NotificationMessage message = found.get();
		LocalDateTime sentAt = LocalDateTime.now();
		if (result.success()) {
			message.markSuccess(result.providerMessageId(), sentAt);
		} else {
			message.markFail(result.failureReason(), sentAt);
		}
		messageRepository.save(message);
	}
}
