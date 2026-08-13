package com.notification.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.notification.api.client.MockProviderClient;
import com.notification.api.domain.MessageStatus;
import com.notification.api.domain.Notification;
import com.notification.api.domain.NotificationChannel;
import com.notification.api.domain.NotificationStatus;
import com.notification.api.domain.OutboxEvent;
import com.notification.api.domain.OutboxStatus;
import com.notification.api.event.NotificationSendEvent;
import com.notification.api.repository.NotificationMessageRepository;
import com.notification.api.repository.NotificationRepository;
import com.notification.api.repository.OutboxEventRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

/**
 * 발송 실행 API가 발송 지시를 outbox에 쌓는지 검증합니다.
 *
 * <p>v2의 발송 실행은 발송사를 호출하지 않습니다. 응답이 돌아온 시점에 아직 아무것도
 * 발송되지 않은 것이 정상이며, 이 테스트는 그 상태를 확인합니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class NotificationDispatchControllerTests {

	private static final String CONTENT = "8월 10일 02시부터 04시까지 점검이 진행됩니다.";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private NotificationRepository notificationRepository;

	@Autowired
	private NotificationMessageRepository messageRepository;

	@Autowired
	private OutboxEventRepository outboxRepository;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private MockProviderClient providerClient;

	@AfterEach
	void clear() {
		outboxRepository.deleteAll();
		messageRepository.deleteAll();
		notificationRepository.deleteAll();
	}

	@Test
	void dispatchQueuesOneOutboxEventPerMessage() throws Exception {
		Long notificationId = saveNotification("user-1", "user-2");

		mockMvc.perform(post("/api/notifications/{id}/dispatch", notificationId))
				.andExpect(status().isAccepted())
				.andExpect(jsonPath("$.code").value("NOTIFICATION_DISPATCH_SUCCESS"))
				.andExpect(jsonPath("$.data.queuedCount").value(2))
				.andExpect(jsonPath("$.data.elapsedMillis").isNumber());

		List<OutboxEvent> events = outboxRepository.findAll();
		assertThat(events).hasSize(2).allSatisfy(event -> {
			assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
			assertThat(event.getPublishedAt()).isNull();
			assertThat(event.getNotificationId()).isEqualTo(notificationId);
		});
		assertThat(events).extracting(event -> payloadOf(event).recipientId())
				.containsExactlyInAnyOrder("user-1", "user-2");
	}

	@Test
	void queuedEventCarriesEverythingNeededToSend() throws Exception {
		Long notificationId = saveNotification("user-1");

		mockMvc.perform(post("/api/notifications/{id}/dispatch", notificationId))
				.andExpect(status().isAccepted());

		OutboxEvent event = outboxRepository.findAll().get(0);
		NotificationSendEvent payload = payloadOf(event);
		assertThat(payload.eventId()).isEqualTo(event.getEventId());
		assertThat(payload.messageId()).isEqualTo(event.getMessageId());
		assertThat(payload.recipientId()).isEqualTo("user-1");
		assertThat(payload.channel()).isEqualTo(NotificationChannel.SMS.name());
		assertThat(payload.content()).isEqualTo(CONTENT);
	}

	@Test
	void dispatchDoesNotSendUntilWorkerPicksEventUp() throws Exception {
		Long notificationId = saveNotification("user-1", "user-2");

		mockMvc.perform(post("/api/notifications/{id}/dispatch", notificationId))
				.andExpect(status().isAccepted());

		then(providerClient).shouldHaveNoInteractions();
		assertThat(messageRepository.findAllByNotificationId(notificationId))
				.allSatisfy(message -> assertThat(message.getStatus()).isEqualTo(MessageStatus.PENDING));

		Notification notification = notificationRepository.findById(notificationId).orElseThrow();
		assertThat(notification.getStatus()).isEqualTo(NotificationStatus.DISPATCHING);
		assertThat(notification.getDispatchStartedAt()).isNotNull();
		assertThat(notification.getDispatchFinishedAt()).isNull();
	}

	@Test
	void dispatchReturnsNotFoundWhenNotificationDoesNotExist() throws Exception {
		mockMvc.perform(post("/api/notifications/{id}/dispatch", 999_999L))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("NOTIFICATION_NOT_FOUND_FAIL"));
	}

	@Test
	void dispatchReturnsConflictWhenAlreadyDispatched() throws Exception {
		Long notificationId = saveNotification("user-1");

		mockMvc.perform(post("/api/notifications/{id}/dispatch", notificationId))
				.andExpect(status().isAccepted());

		mockMvc.perform(post("/api/notifications/{id}/dispatch", notificationId))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("NOTIFICATION_ALREADY_DISPATCHED_FAIL"))
				.andExpect(jsonPath("$.message").value("이미 발송을 시작한 요청입니다."));

		assertThat(outboxRepository.findAll()).hasSize(1);
	}

	private NotificationSendEvent payloadOf(OutboxEvent event) {
		return objectMapper.readValue(event.getPayload(), NotificationSendEvent.class);
	}

	private Long saveNotification(String... recipientIds) {
		Notification notification = notificationRepository.save(Notification.create(
				"8월 정기 점검 안내",
				CONTENT,
				NotificationChannel.SMS,
				LocalDateTime.now().plusHours(1)
		));
		for (String recipientId : recipientIds) {
			messageRepository.save(notification.addMessage(recipientId));
		}
		return notification.getId();
	}
}
