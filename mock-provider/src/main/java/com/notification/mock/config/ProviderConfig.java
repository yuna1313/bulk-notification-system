package com.notification.mock.config;

/**
 * mock-provider가 외부 발송사처럼 동작할 때 사용할 설정값을 표현합니다.
 * 이 값은 DB에 저장하지 않고 메모리에서만 관리되며, /send API의 지연 시간, 실패율, 초당 요청 제한을 결정합니다.
 *
 * @param latencyMs 응답 전까지 대기 시간
 * @param failureRate 실패 응답 처리 확률
 * @param rateLimitPerSecond 1초 동안 허용할 최대 요청 수
 */
public record ProviderConfig(
		long latencyMs,
		double failureRate,
		int rateLimitPerSecond
) {

	/**
	 * 서버 시작 시 사용할 기본 mock-provider 설정값을 생성합니다.
	 *
	 * @return 기본 지연 시간, 실패율, 초당 요청 제한이 담긴 설정값
	 */
	public static ProviderConfig defaults() {
		return new ProviderConfig(200, 0.03, 1000);
	}
}
