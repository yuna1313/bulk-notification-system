package com.notification.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * 엔티티의 생성 시각과 수정 시각이 자동으로 채워지도록 JPA Auditing을 활성화합니다.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
