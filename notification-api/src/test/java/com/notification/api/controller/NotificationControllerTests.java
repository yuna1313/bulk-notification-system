package com.notification.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.notification.api.domain.MessageStatus;
import com.notification.api.domain.NotificationMessage;
import com.notification.api.domain.NotificationStatus;
import com.notification.api.repository.NotificationMessageRepository;
import com.notification.api.repository.NotificationRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class NotificationControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private NotificationRepository notificationRepository;

	@Autowired
	private NotificationMessageRepository messageRepository;

	@Test
	void createStoresNotificationAndMessagePerRecipient() throws Exception {
		mockMvc.perform(post("/api/notifications")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "8월 정기 점검 안내",
								  "content": "8월 10일 02시부터 04시까지 점검이 진행됩니다.",
								  "channel": "SMS",
								  "scheduledAt": "2026-08-10T02:00:00",
								  "recipientIds": ["user-1", "user-2", "user-3"]
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.code").value("NOTIFICATION_CREATE_SUCCESS"))
				.andExpect(jsonPath("$.message").value("발송 요청을 접수하였습니다."))
				.andExpect(jsonPath("$.data.notificationId").isNumber())
				.andExpect(jsonPath("$.data.recipientCount").value(3));

		assertThat(notificationRepository.findAll()).singleElement()
				.satisfies(notification -> assertThat(notification.getStatus()).isEqualTo(NotificationStatus.PENDING));

		List<NotificationMessage> messages = messageRepository.findAll();
		assertThat(messages).hasSize(3);
		assertThat(messages).allSatisfy(message ->
				assertThat(message.getStatus()).isEqualTo(MessageStatus.PENDING));
		assertThat(messages).extracting(NotificationMessage::getRecipientId)
				.containsExactlyInAnyOrder("user-1", "user-2", "user-3");
	}

	@Test
	void createAcceptsPastScheduledAt() throws Exception {
		mockMvc.perform(post("/api/notifications")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "지난 공지",
								  "content": "예약 시각이 과거인 요청입니다.",
								  "channel": "EMAIL",
								  "scheduledAt": "2020-01-01T00:00:00",
								  "recipientIds": ["user-1"]
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.recipientCount").value(1));
	}

	@Test
	void createReturnsBadRequestWhenScheduledAtIsNull() throws Exception {
		mockMvc.perform(post("/api/notifications")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "예약 시각 없음",
								  "content": "예약 시각을 빠뜨린 요청입니다.",
								  "channel": "SMS",
								  "recipientIds": ["user-1"]
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST_FAIL"))
				.andExpect(jsonPath("$.data.scheduledAt").value("예약 시각은 필수입니다."));
	}

	@Test
	void createReturnsBadRequestWhenRecipientIdsAreEmpty() throws Exception {
		mockMvc.perform(post("/api/notifications")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "수신자 없음",
								  "content": "수신자를 빠뜨린 요청입니다.",
								  "channel": "SMS",
								  "scheduledAt": "2026-08-10T02:00:00",
								  "recipientIds": []
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST_FAIL"))
				.andExpect(jsonPath("$.data.recipientIds").value("수신자는 한 명 이상이어야 합니다."));
	}

	@Test
	void createReturnsBadRequestWhenChannelIsUnknown() throws Exception {
		mockMvc.perform(post("/api/notifications")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "잘못된 발송 수단",
								  "content": "정의되지 않은 채널을 보낸 요청입니다.",
								  "channel": "FAX",
								  "scheduledAt": "2026-08-10T02:00:00",
								  "recipientIds": ["user-1"]
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST_FAIL"));
	}
}
