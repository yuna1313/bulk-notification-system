package com.notification.worker.client;

import com.notification.worker.client.dto.ProviderApiResponse;
import com.notification.worker.client.dto.ProviderSendRequest;
import com.notification.worker.client.dto.ProviderSendResponse;
import com.notification.worker.domain.FailureReason;
import java.net.BindException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
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
 * <p>v1에서 notification-api가 쓰던 것을 그대로 옮겨왔습니다. 발송사 호출 자체는 v1과 v2가
 * 같아야 두 구조의 측정값을 비교할 수 있기 때문에 동작을 바꾸지 않았습니다.
 *
 * <p>재시도는 하지 않습니다. 실패를 그대로 결과로 돌려주며, 재시도는 컨슈머 단계에서
 * 따로 다룹니다.
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
			FailureReason failureReason = toNetworkFailureReason(exception);
			log.warn("[send] 발송사에 접근하지 못하였습니다. messageId={}, failureReason={}",
					request.messageId(), failureReason, exception);
			return ProviderSendResult.fail(failureReason);
		} catch (RestClientException exception) {
			log.warn("[send] 발송사 호출에 실패하였습니다. messageId={}", request.messageId(), exception);
			return ProviderSendResult.fail(FailureReason.UNKNOWN);
		}
	}

	/**
	 * 발송사에 접근하지 못한 원인을 실패 사유로 분류합니다.
	 *
	 * <p>{@link ResourceAccessException}은 응답 지연과 연결 실패를 모두 감싸고 있어
	 * 원인 예외까지 확인해야 둘을 구분할 수 있습니다. 발송사가 느린 것과
	 * 연결 자체가 안 되는 것은 부하 테스트 결과 해석에서 전혀 다른 의미를 가집니다.
	 *
	 * @param exception 발송사 호출 중 발생한 예외
	 * @return 원인에 해당하는 실패 사유
	 */
	static FailureReason toNetworkFailureReason(Throwable exception) {
		for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
			if (cause instanceof SocketTimeoutException) {
				return FailureReason.TIMEOUT;
			}
			if (cause instanceof BindException || cause instanceof ConnectException) {
				return FailureReason.CONNECT_FAIL;
			}
			if (cause == cause.getCause()) {
				break;
			}
		}
		return FailureReason.UNKNOWN;
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
