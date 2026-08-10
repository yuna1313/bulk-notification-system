package com.notification.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.notification.api.domain.NotificationMessage;
import com.notification.api.repository.NotificationMessageRepository;
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
class TestNotificationControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private NotificationMessageRepository messageRepository;

	@Test
	void createGeneratesSequentialRecipientIds() throws Exception {
		mockMvc.perform(post("/api/test/notifications")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "부하 테스트",
								  "content": "부하 테스트용 발송 요청입니다.",
								  "channel": "SMS",
								  "scheduledAt": "2026-08-10T02:00:00",
								  "recipientCount": 3
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.code").value("NOTIFICATION_CREATE_SUCCESS"))
				.andExpect(jsonPath("$.data.recipientCount").value(3));

		assertThat(messageRepository.findAll()).extracting(NotificationMessage::getRecipientId)
				.containsExactlyInAnyOrder("user-1", "user-2", "user-3");
	}

	@Test
	void createReturnsBadRequestWhenRecipientCountIsZero() throws Exception {
		mockMvc.perform(post("/api/test/notifications")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "부하 테스트",
								  "content": "수신자 수가 0인 요청입니다.",
								  "channel": "SMS",
								  "scheduledAt": "2026-08-10T02:00:00",
								  "recipientCount": 0
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST_FAIL"))
				.andExpect(jsonPath("$.data.recipientCount").value("수신자 수는 1 이상이어야 합니다."));
	}

	@Test
	void createReturnsBadRequestWhenRecipientCountIsMissing() throws Exception {
		mockMvc.perform(post("/api/test/notifications")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "부하 테스트",
								  "content": "수신자 수를 빠뜨린 요청입니다.",
								  "channel": "SMS",
								  "scheduledAt": "2026-08-10T02:00:00"
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.data.recipientCount").value("수신자 수는 필수입니다."));
	}
}
