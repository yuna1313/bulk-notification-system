package com.notification.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.notification.api.config.KafkaTopicProperties;
import com.notification.api.domain.OutboxEvent;
import com.notification.api.domain.OutboxStatus;
import com.notification.api.repository.OutboxEventRepository;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * outbox 발행 poller가 대기 이벤트를 Kafka로 내보내고 상태를 바꾸는지 검증합니다.
 *
 * <p>브로커를 띄우지 않고 {@link KafkaTemplate}을 대역으로 바꿔 검증합니다. 확인하려는 것은
 * 브로커 동작이 아니라 "무엇을 어떤 키로 보내고, 결과에 따라 상태를 어떻게 바꾸는가"이기
 * 때문입니다.
 */
@SpringBootTest
class OutboxPublishServiceTests {

	@Autowired
	private OutboxPublishService publishService;

	@Autowired
	private OutboxEventRepository outboxRepository;

	@Autowired
	private KafkaTopicProperties topicProperties;

	@MockitoBean
	private KafkaTemplate<String, String> kafkaTemplate;

	@BeforeEach
	@AfterEach
	void clear() {
		outboxRepository.deleteAll();
	}

	@Test
	void publishesPendingEventsAndMarksThemPublished() {
		saveEvent(1L, 11L);
		saveEvent(1L, 12L);
		givenSendSucceeds();

		int publishedCount = publishService.publishPending();

		assertThat(publishedCount).isEqualTo(2);
		assertThat(outboxRepository.findAll()).allSatisfy(event -> {
			assertThat(event.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
			assertThat(event.getPublishedAt()).isNotNull();
		});
	}

	@Test
	void sendsPayloadToSendTopicKeyedByMessageId() {
		saveEvent(1L, 11L);
		givenSendSucceeds();

		publishService.publishPending();

		ArgumentCaptor<String> topic = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
		then(kafkaTemplate).should().send(topic.capture(), key.capture(), payload.capture());

		assertThat(topic.getValue()).isEqualTo(topicProperties.name());
		assertThat(key.getValue()).isEqualTo("11");
		assertThat(payload.getValue()).contains("\"messageId\":11");
	}

	@Test
	void leavesEventPendingWhenPublishFails() {
		saveEvent(1L, 11L);
		saveEvent(1L, 12L);
		given(kafkaTemplate.send(anyString(), anyString(), anyString()))
				.willReturn(CompletableFuture.completedFuture(null))
				.willReturn(CompletableFuture.failedFuture(new IllegalStateException("브로커 응답 없음")));

		int publishedCount = publishService.publishPending();

		assertThat(publishedCount).isEqualTo(1);
		List<OutboxEvent> events = outboxRepository.findAll();
		assertThat(events).filteredOn(event -> event.getMessageId() == 11L)
				.singleElement()
				.satisfies(event -> assertThat(event.getStatus()).isEqualTo(OutboxStatus.PUBLISHED));
		assertThat(events).filteredOn(event -> event.getMessageId() == 12L)
				.singleElement()
				.satisfies(event -> {
					assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
					assertThat(event.getPublishedAt()).isNull();
				});
	}

	@Test
	void doesNothingWhenNoEventIsPending() {
		int publishedCount = publishService.publishPending();

		assertThat(publishedCount).isZero();
		then(kafkaTemplate).should(never()).send(anyString(), anyString(), anyString());
	}

	private void givenSendSucceeds() {
		given(kafkaTemplate.send(anyString(), anyString(), anyString()))
				.willReturn(CompletableFuture.completedFuture(null));
	}

	private void saveEvent(Long notificationId, Long messageId) {
		String eventId = UUID.randomUUID().toString();
		String payload = """
				{"eventId":"%s","notificationId":%d,"messageId":%d,\
				"recipientId":"user-%d","channel":"SMS","content":"점검 안내"}\
				""".formatted(eventId, notificationId, messageId, messageId);
		outboxRepository.save(OutboxEvent.create(eventId, notificationId, messageId, payload));
	}
}
