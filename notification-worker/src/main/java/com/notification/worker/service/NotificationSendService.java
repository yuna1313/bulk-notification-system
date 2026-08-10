package com.notification.worker.service;

import com.notification.worker.client.MockProviderClient;
import com.notification.worker.client.ProviderSendResult;
import com.notification.worker.client.dto.ProviderSendRequest;
import com.notification.worker.domain.NotificationMessage;
import com.notification.worker.event.NotificationSendEvent;
import com.notification.worker.repository.NotificationMessageRepository;
import com.notification.worker.repository.ProcessedEventRepository;
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
	private final ProcessedEventRepository processedEventRepository;

	/**
	 * 발송 지시를 받아 발송사를 호출하고 결과를 발송 건에 기록합니다.
	 *
	 * <p>발송 전에 처리 기록을 남겨 같은 지시가 두 번 나가지 않게 합니다. 기록에 실패하면
	 * 이미 처리한 지시이므로 발송하지 않고 끝냅니다.
	 *
	 * <p>기록과 발송 사이에 프로세스가 죽으면 그 건은 기록만 남고 발송되지 않습니다.
	 * 중복 발송을 막는 대신 감수한 부분이며, 이렇게 남은 건은 발송 건이 계속
	 * {@code PENDING}이므로 구간 재처리로 다시 보낼 수 있습니다. 재처리는 이벤트 식별자를
	 * 새로 발급하므로 이 기록에 걸리지 않습니다.
	 *
	 * @param event 발송 지시
	 */
	public void send(NotificationSendEvent event) {
		if (!processedEventRepository.claim(event.eventId(), event.messageId(), LocalDateTime.now())) {
			log.info("[send] 이미 처리한 발송 지시라 건너뜁니다. eventId={}, messageId={}",
					event.eventId(), event.messageId());
			return;
		}

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
