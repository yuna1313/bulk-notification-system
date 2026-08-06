package com.notification.mock;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProviderConfigControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void getConfigReturnsDefaultValues() throws Exception {
		mockMvc.perform(get("/config"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.code").value("CONFIG_GET_SUCCESS"))
				.andExpect(jsonPath("$.message").value("mock-provider 설정 조회를 성공하였습니다."))
				.andExpect(jsonPath("$.data.latencyMs").value(200))
				.andExpect(jsonPath("$.data.failureRate").value(0.03))
				.andExpect(jsonPath("$.data.rateLimitPerSecond").value(1000));
	}
}
