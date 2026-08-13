package com.notification.api.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.notification.api.client.MockProviderClient;
import com.notification.api.domain.Notification;
import com.notification.api.domain.NotificationChannel;
import com.notification.api.repository.NotificationMessageRepository;
import com.notification.api.repository.NotificationRepository;
import com.notification.api.repository.OutboxEventRepository;
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

	@Autowired
	private OutboxEventRepository outboxRepository;

	@MockitoBean
	private MockProviderClient providerClient;

	@AfterEach
	void clear() {
		outboxRepository.deleteAll();
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

	/**
	 * 발송 실행 직후에는 발송 지시를 쌓기만 한 상태이므로 건수가 하나도 움직이지 않아야 합니다.
	 *
	 * <p>완료 시각과 소요시간은 워커가 모든 발송 건을 처리한 뒤에 채워지며, 그 처리는
	 * 아직 만들지 않았습니다.
	 */
	@Test
	void getStatusReturnsEveryMessageAsPendingRightAfterDispatch() throws Exception {
		Long notificationId = saveNotification("user-1", "user-2", "user-3");

		mockMvc.perform(post("/api/notifications/{id}/dispatch", notificationId))
				.andExpect(status().isAccepted());

		mockMvc.perform(get("/api/notifications/{id}", notificationId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("DISPATCHING"))
				.andExpect(jsonPath("$.data.totalCount").value(3))
				.andExpect(jsonPath("$.data.successCount").value(0))
				.andExpect(jsonPath("$.data.failCount").value(0))
				.andExpect(jsonPath("$.data.pendingCount").value(3))
				.andExpect(jsonPath("$.data.dispatchStartedAt").exists())
				.andExpect(jsonPath("$.data.dispatchFinishedAt").doesNotExist())
				.andExpect(jsonPath("$.data.elapsedMillis").doesNotExist());
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
