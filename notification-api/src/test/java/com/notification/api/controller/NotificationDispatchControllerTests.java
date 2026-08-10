package com.notification.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.notification.api.client.MockProviderClient;
import com.notification.api.client.ProviderSendResult;
import com.notification.api.client.dto.ProviderSendRequest;
import com.notification.api.domain.FailureReason;
import com.notification.api.domain.MessageStatus;
import com.notification.api.domain.Notification;
import com.notification.api.domain.NotificationChannel;
import com.notification.api.domain.NotificationMessage;
import com.notification.api.domain.NotificationStatus;
import com.notification.api.repository.NotificationMessageRepository;
import com.notification.api.repository.NotificationRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class NotificationDispatchControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private NotificationRepository notificationRepository;

	@Autowired
	private NotificationMessageRepository messageRepository;

	@MockitoBean
	private MockProviderClient providerClient;

	@AfterEach
	void clear() {
		messageRepository.deleteAll();
		notificationRepository.deleteAll();
	}

	@Test
	void dispatchMarksEveryMessageAsSuccessWhenProviderAccepts() throws Exception {
		given(providerClient.send(any(ProviderSendRequest.class)))
				.willReturn(ProviderSendResult.success("provider-message-1"));
		Long notificationId = saveNotification("user-1", "user-2");

		mockMvc.perform(post("/api/notifications/{id}/dispatch", notificationId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value("NOTIFICATION_DISPATCH_SUCCESS"))
				.andExpect(jsonPath("$.data.totalCount").value(2))
				.andExpect(jsonPath("$.data.successCount").value(2))
				.andExpect(jsonPath("$.data.failCount").value(0))
				.andExpect(jsonPath("$.data.elapsedMillis").isNumber());

		assertThat(messageRepository.findAllByNotificationId(notificationId))
				.allSatisfy(message -> {
					assertThat(message.getStatus()).isEqualTo(MessageStatus.SUCCESS);
					assertThat(message.getProviderMessageId()).isEqualTo("provider-message-1");
					assertThat(message.getSentAt()).isNotNull();
				});

		Notification notification = notificationRepository.findById(notificationId).orElseThrow();
		assertThat(notification.getStatus()).isEqualTo(NotificationStatus.COMPLETED);
		assertThat(notification.getDispatchStartedAt()).isNotNull();
		assertThat(notification.getDispatchFinishedAt()).isNotNull();
	}

	@Test
	void dispatchReturnsOkAndRecordsFailureReasonWhenProviderRejects() throws Exception {
		given(providerClient.send(any(ProviderSendRequest.class)))
				.willReturn(ProviderSendResult.success("provider-message-1"))
				.willReturn(ProviderSendResult.fail(FailureReason.RATE_LIMIT));
		Long notificationId = saveNotification("user-1", "user-2");

		mockMvc.perform(post("/api/notifications/{id}/dispatch", notificationId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.successCount").value(1))
				.andExpect(jsonPath("$.data.failCount").value(1));

		List<NotificationMessage> messages = messageRepository.findAllByNotificationId(notificationId);
		assertThat(messages).filteredOn(message -> message.getStatus() == MessageStatus.FAIL)
				.singleElement()
				.satisfies(message ->
						assertThat(message.getFailureReason()).isEqualTo(FailureReason.RATE_LIMIT));
	}

	@Test
	void dispatchReturnsNotFoundWhenNotificationDoesNotExist() throws Exception {
		mockMvc.perform(post("/api/notifications/{id}/dispatch", 999_999L))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("NOTIFICATION_NOT_FOUND_FAIL"));
	}

	@Test
	void dispatchReturnsConflictWhenAlreadyDispatched() throws Exception {
		given(providerClient.send(any(ProviderSendRequest.class)))
				.willReturn(ProviderSendResult.success("provider-message-1"));
		Long notificationId = saveNotification("user-1");

		mockMvc.perform(post("/api/notifications/{id}/dispatch", notificationId))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/notifications/{id}/dispatch", notificationId))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("NOTIFICATION_ALREADY_DISPATCHED_FAIL"))
				.andExpect(jsonPath("$.message").value("이미 발송을 시작한 요청입니다."));
	}

	private Long saveNotification(String... recipientIds) {
		Notification notification = notificationRepository.save(Notification.create(
				"8월 정기 점검 안내",
				"8월 10일 02시부터 04시까지 점검이 진행됩니다.",
				NotificationChannel.SMS,
				LocalDateTime.now().plusHours(1)
		));
		for (String recipientId : recipientIds) {
			messageRepository.save(notification.addMessage(recipientId));
		}
		return notification.getId();
	}
}
