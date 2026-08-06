package com.notification.mock.service;

import com.notification.mock.common.SendResponseCode;
import com.notification.mock.dto.ProviderConfig;
import com.notification.mock.dto.SendRequest;
import com.notification.mock.dto.SendResponse;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 외부 발송사의 지연, 실패, 요청 제한을 동기 방식으로 재현합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MockSendService {

	private static final String SUCCESS_STATUS = "SUCCESS";

	private final ProviderConfigStore configStore;
	private final RateLimiter rateLimiter;

	/**
	 * 발송 요청을 처리하고 현재 설정에 따라 성공, 실패, 요청 제한 결과를 반환합니다.
	 *
	 * @param request 발송 요청 정보
	 * @return mock-provider 발송 처리 결과
	 */
	public SendResult send(SendRequest request) {
		ProviderConfig config = configStore.get();
		if (!rateLimiter.tryAcquire(config.rateLimitPerSecond())) {
			log.warn(
					"[send] 초당 요청 한도 초과: messageId={}, recipientId={}, rateLimitPerSecond={}",
					request.getMessageId(),
					request.getRecipientId(),
					config.rateLimitPerSecond()
			);
			return SendResult.fail(SendResponseCode.SEND_RATE_LIMIT_FAIL);
		}

		waitLatency(config.latencyMs());

		if (isFailure(config.failureRate())) {
			log.warn(
					"[send] 설정된 실패율에 따른 발송 실패: messageId={}, recipientId={}, failureRate={}",
					request.getMessageId(),
					request.getRecipientId(),
					config.failureRate()
			);
			return SendResult.fail(SendResponseCode.SEND_FAIL);
		}

		return SendResult.success(new SendResponse(
				request.getMessageId(),
				SUCCESS_STATUS,
				UUID.randomUUID().toString()
		));
	}

	private void waitLatency(long latencyMs) {
		try {
			Thread.sleep(latencyMs);
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
		}
	}

	private boolean isFailure(double failureRate) {
		return ThreadLocalRandom.current().nextDouble() < failureRate;
	}
}
