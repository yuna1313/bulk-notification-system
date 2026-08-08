package com.notification.mock;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SendControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
	void sendReturnsSuccessResponse() throws Exception {
		updateConfig(0, 0.0, 1000);

		mockMvc.perform(post("/send")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "messageId": "message-1",
								  "recipientId": "recipient-1",
								  "channel": "SMS",
								  "content": "hello"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value("SEND_SUCCESS"))
				.andExpect(jsonPath("$.message").value("발송 요청 처리를 성공하였습니다."))
				.andExpect(jsonPath("$.data.messageId").value("message-1"))
				.andExpect(jsonPath("$.data.status").value("SUCCESS"))
				.andExpect(jsonPath("$.data.providerMessageId").isNotEmpty());
	}

	@Test
	@DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
	void sendReturnsFailResponseWhenFailureRateIsOne() throws Exception {
		updateConfig(0, 1.0, 1000);

		mockMvc.perform(post("/send")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "messageId": "message-1",
								  "recipientId": "recipient-1",
								  "channel": "SMS",
								  "content": "hello"
								}
								"""))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.code").value("SEND_FAIL"))
				.andExpect(jsonPath("$.message").value("발송 요청 처리에 실패하였습니다."));
	}

	@Test
	@DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
	void sendReturnsRateLimitFailResponseWhenLimitIsExceeded() throws Exception {
		updateConfig(0, 0.0, 1);

		sendRequest("message-1")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value("SEND_SUCCESS"));

		sendRequest("message-2")
				.andExpect(status().isTooManyRequests())
				.andExpect(jsonPath("$.code").value("SEND_RATE_LIMIT_FAIL"))
				.andExpect(jsonPath("$.message").value("초당 요청 한도를 초과하였습니다."));
	}

	private void updateConfig(long latencyMs, double failureRate, int rateLimitPerSecond) throws Exception {
		mockMvc.perform(put("/config")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "latencyMs": %d,
						  "failureRate": %s,
						  "rateLimitPerSecond": %d
						}
						""".formatted(latencyMs, failureRate, rateLimitPerSecond)));
	}

	private org.springframework.test.web.servlet.ResultActions sendRequest(String messageId) throws Exception {
		return mockMvc.perform(post("/send")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "messageId": "%s",
						  "recipientId": "recipient-1",
						  "channel": "SMS",
						  "content": "hello"
						}
						""".formatted(messageId)));
	}
}
