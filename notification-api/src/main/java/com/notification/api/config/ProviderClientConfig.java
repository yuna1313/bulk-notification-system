package com.notification.api.config;

import com.notification.api.client.ProviderProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * 외부 발송사를 호출할 HTTP 클라이언트를 설정합니다.
 */
@Configuration
@EnableConfigurationProperties(ProviderProperties.class)
public class ProviderClientConfig {

	/**
	 * 발송사 주소와 제한 시간이 적용된 HTTP 클라이언트를 생성합니다.
	 *
	 * <p>응답 대기 제한 시간은 발송사가 재현하는 지연 시간보다 길게 두어야 합니다.
	 * 그렇지 않으면 지연 재현이 전부 타임아웃 실패로 기록됩니다.
	 *
	 * @param properties 발송사 접속 설정
	 * @return 발송사 호출용 HTTP 클라이언트
	 */
	@Bean
	public RestClient providerRestClient(ProviderProperties properties) {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(properties.connectTimeout());
		requestFactory.setReadTimeout(properties.readTimeout());

		return RestClient.builder()
				.baseUrl(properties.baseUrl())
				.requestFactory(requestFactory)
				.build();
	}
}
