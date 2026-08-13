package com.notification.api.service;

import com.notification.api.config.KafkaTopicProperties;
import com.notification.api.config.OutboxPublishProperties;
import com.notification.api.domain.OutboxEvent;
import com.notification.api.domain.OutboxStatus;
import com.notification.api.repository.OutboxEventRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

/**
 * outbox에 쌓인 발행 대기 이벤트를 Kafka로 내보냅니다.
 *
 * <p>DB에 적어둔 발송 지시를 실제로 Kafka에 넣는 유일한 경로입니다. 발송 실행 API는 outbox에
 * 쓰기만 하고 Kafka를 건드리지 않으므로, 이 클래스가 돌지 않으면 아무것도 발송되지 않습니다.
 *
 * <p>메서드 전체를 트랜잭션으로 묶지 않습니다. 묶으면 Kafka로 보내는 내내 DB 커넥션을 붙잡고
 * 있게 되어, 발행이 느려질수록 커넥션 풀이 말라붙습니다. 대신 조회와 상태 변경을 각각 짧은
 * 트랜잭션으로 끊고 그 사이의 발행은 트랜잭션 밖에서 처리합니다.
 *
 * <p>그 결과 발행에 성공하고 상태를 바꾸기 전에 죽으면 같은 이벤트가 다시 발행됩니다.
 * 유실과 중복 중 중복을 택한 것이며, 중복 제거는 이벤트 식별자를 받는 워커 쪽 책임입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxPublishService {

	private final OutboxEventRepository outboxRepository;
	private final KafkaTemplate<String, String> kafkaTemplate;
	private final KafkaTopicProperties topicProperties;
	private final OutboxPublishProperties publishProperties;

	/**
	 * 발행 대기 이벤트를 한 묶음 읽어 Kafka로 발행합니다.
	 *
	 * <p>발행에 실패한 이벤트는 대기 상태로 남겨둡니다. 다음 주기가 다시 집어가므로 이 자리에서
	 * 재시도하지 않습니다.
	 *
	 * @return 발행에 성공한 건수
	 */
	public int publishPending() {
		List<OutboxEvent> events = outboxRepository.findByStatusOrderByIdAsc(
				OutboxStatus.PENDING, Limit.of(publishProperties.batchSize()));
		if (events.isEmpty()) {
			return 0;
		}

		List<Long> publishedIds = send(events);
		if (!publishedIds.isEmpty()) {
			outboxRepository.markPublished(publishedIds, LocalDateTime.now());
		}

		int failCount = events.size() - publishedIds.size();
		if (failCount > 0) {
			log.warn("[outbox] 발행 실패가 있습니다. 다음 주기에 다시 시도합니다. read={}, published={}, fail={}",
					events.size(), publishedIds.size(), failCount);
		} else {
			log.debug("[outbox] 발행 완료: published={}", publishedIds.size());
		}
		return publishedIds.size();
	}

	/**
	 * 묶음 전체를 먼저 보내고 나서 결과를 기다립니다.
	 *
	 * <p>한 건씩 보내고 응답을 기다리면 브로커 왕복 시간이 건수만큼 그대로 쌓입니다.
	 * 먼저 다 밀어 넣으면 그 시간이 겹쳐서 흐릅니다.
	 */
	private List<Long> send(List<OutboxEvent> events) {
		List<CompletableFuture<SendResult<String, String>>> futures = new ArrayList<>(events.size());
		for (OutboxEvent event : events) {
			futures.add(kafkaTemplate.send(
					topicProperties.name(),
					String.valueOf(event.getMessageId()),
					event.getPayload()
			));
		}

		List<Long> publishedIds = new ArrayList<>(events.size());
		for (int index = 0; index < events.size(); index++) {
			OutboxEvent event = events.get(index);
			try {
				futures.get(index).join();
				publishedIds.add(event.getId());
			} catch (RuntimeException e) {
				log.warn("[outbox] 이벤트 발행에 실패했습니다. outboxId={}, eventId={}",
						event.getId(), event.getEventId(), e);
			}
		}
		return publishedIds;
	}
}
