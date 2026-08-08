package com.notification.mock.config;

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
	 * mock-provider API 문서의 제목과 설명을 설정합니다.
	 *
	 * @return Swagger UI에서 사용할 OpenAPI 설정 객체
	 */
	@Bean
	public OpenAPI openAPI() {
		return new OpenAPI()
				.info(new Info()
						.title("Mock Provider API")
						.description("외부 발송사의 지연, 실패, 요청 제한을 재현하는 테스트용 API")
						.version("v1"));
	}
}
