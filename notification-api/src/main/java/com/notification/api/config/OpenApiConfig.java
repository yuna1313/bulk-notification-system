package com.notification.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI에 표시할 OpenAPI 문서 기본 정보를 설정합니다.
 */
@Configuration
public class OpenApiConfig {

	/**
	 * notification-api 문서의 제목과 설명을 설정합니다.
	 *
	 * @return Swagger UI에서 사용할 OpenAPI 설정 객체
	 */
	@Bean
	public OpenAPI openAPI() {
		return new OpenAPI()
				.info(new Info()
						.title("Notification API")
						.description("예약 대량 알림의 발송 요청 접수와 발송 현황 조회를 제공하는 API")
						.version("v1"));
	}
}
