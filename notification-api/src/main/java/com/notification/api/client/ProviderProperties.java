package com.notification.api.client;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 외부 발송사 접속 설정입니다.
 *
 * @param baseUrl 발송사 서버 주소
 * @param connectTimeout 연결 제한 시간
 * @param readTimeout 응답 대기 제한 시간. 발송사가 지연을 재현하는 시간보다 길어야 합니다.
 */
@ConfigurationProperties(prefix = "notification.provider")
public record ProviderProperties(
		String baseUrl,
		Duration connectTimeout,
		Duration readTimeout
) {
}
