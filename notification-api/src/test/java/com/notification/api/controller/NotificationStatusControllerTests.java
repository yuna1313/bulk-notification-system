package com.notification.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.notification.api.client.MockProviderClient;
import com.notification.api.client.ProviderSendResult;
import com.notification.api.client.dto.ProviderSendRequest;
import com.notification.api.domain.FailureReason;
import com.notification.api.domain.Notification;
import com.notification.api.domain.NotificationChannel;
import com.notification.api.repository.NotificationMessageRepository;
import com.notification.api.repository.NotificationRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class NotificationStatusControllerTests {

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
	void getStatusReturnsEveryMessageAsPendingBeforeDispatch() throws Exception {
		Long notificationId = saveNotification("user-1", "user-2", "user-3");

		mockMvc.perform(get("/api/notifications/{id}", notificationId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value("NOTIFICATION_GET_SUCCESS"))
				.andExpect(jsonPath("$.data.status").value("PENDING"))
				.andExpect(jsonPath("$.data.totalCount").value(3))
				.andExpect(jsonPath("$.data.successCount").value(0))
				.andExpect(jsonPath("$.data.failCount").value(0))
				.andExpect(jsonPath("$.data.pendingCount").value(3))
				.andExpect(jsonPath("$.data.dispatchStartedAt").doesNotExist())
				.andExpect(jsonPath("$.data.elapsedMillis").doesNotExist());
	}

	@Test
	void getStatusReturnsSuccessAndFailCountAfterDispatch() throws Exception {
		given(providerClient.send(any(ProviderSendRequest.class)))
				.willReturn(ProviderSendResult.success("provider-message-1"))
				.willReturn(ProviderSendResult.fail(FailureReason.RATE_LIMIT))
				.willReturn(ProviderSendResult.success("provider-message-3"));
		Long notificationId = saveNotification("user-1", "user-2", "user-3");

		mockMvc.perform(post("/api/notifications/{id}/dispatch", notificationId))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/notifications/{id}", notificationId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("COMPLETED"))
				.andExpect(jsonPath("$.data.totalCount").value(3))
				.andExpect(jsonPath("$.data.successCount").value(2))
				.andExpect(jsonPath("$.data.failCount").value(1))
				.andExpect(jsonPath("$.data.pendingCount").value(0))
				.andExpect(jsonPath("$.data.dispatchStartedAt").exists())
				.andExpect(jsonPath("$.data.dispatchFinishedAt").exists())
				.andExpect(jsonPath("$.data.elapsedMillis").isNumber());
	}

	@Test
	void getStatusReturnsNotFoundWhenNotificationDoesNotExist() throws Exception {
		mockMvc.perform(get("/api/notifications/{id}", 999_999L))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("NOTIFICATION_NOT_FOUND_FAIL"));
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
