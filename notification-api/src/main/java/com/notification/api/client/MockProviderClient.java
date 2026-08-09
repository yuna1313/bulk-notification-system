package com.notification.api.client;

import com.notification.api.client.dto.ProviderApiResponse;
import com.notification.api.client.dto.ProviderSendRequest;
import com.notification.api.client.dto.ProviderSendResponse;
import com.notification.api.domain.FailureReason;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 외부 발송사에 발송 요청을 한 건씩 보냅니다.
 *
 * <p>v1은 재시도하지 않습니다. 호출이 실패하면 그 결과를 그대로 실패 사유로 변환해 돌려줍니다.
 */
@Slf4j
@Component
public class MockProviderClient {

	private static final String SEND_PATH = "/send";

	private final RestClient restClient;

	public MockProviderClient(RestClient providerRestClient) {
		this.restClient = providerRestClient;
	}

	/**
	 * 발송 요청 한 건을 외부 발송사로 보냅니다.
	 *
	 * @param request 발송 요청 정보
	 * @return 성공 여부와 실패 사유가 담긴 호출 결과
	 */
	public ProviderSendResult send(ProviderSendRequest request) {
		try {
			ResponseEntity<ProviderApiResponse<ProviderSendResponse>> response = restClient.post()
					.uri(SEND_PATH)
					.body(request)
					.retrieve()
					.onStatus(HttpStatusCode::isError, (failedRequest, failedResponse) -> {
					})
					.toEntity(new ParameterizedTypeReference<>() {
					});

			return toResult(request, response);
		} catch (ResourceAccessException exception) {
			log.warn("[send] 발송사 응답 시간을 초과하였습니다. messageId={}", request.messageId(), exception);
			return ProviderSendResult.fail(FailureReason.TIMEOUT);
		} catch (RestClientException exception) {
			log.warn("[send] 발송사 호출에 실패하였습니다. messageId={}", request.messageId(), exception);
			return ProviderSendResult.fail(FailureReason.UNKNOWN);
		}
	}

	private ProviderSendResult toResult(
			ProviderSendRequest request,
			ResponseEntity<ProviderApiResponse<ProviderSendResponse>> response
	) {
		HttpStatusCode statusCode = response.getStatusCode();
		ProviderApiResponse<ProviderSendResponse> body = response.getBody();

		if (statusCode.is2xxSuccessful() && body != null && body.data() != null) {
			return ProviderSendResult.success(body.data().providerMessageId());
		}

		FailureReason failureReason = toFailureReason(statusCode);
		log.warn("[send] 발송사가 실패로 응답하였습니다. messageId={}, status={}, failureReason={}",
				request.messageId(), statusCode.value(), failureReason);
		return ProviderSendResult.fail(failureReason);
	}

	private FailureReason toFailureReason(HttpStatusCode statusCode) {
		if (statusCode.isSameCodeAs(HttpStatus.TOO_MANY_REQUESTS)) {
			return FailureReason.RATE_LIMIT;
		}
		if (statusCode.is5xxServerError()) {
			return FailureReason.PROVIDER_FAIL;
		}
		return FailureReason.UNKNOWN;
	}
}
