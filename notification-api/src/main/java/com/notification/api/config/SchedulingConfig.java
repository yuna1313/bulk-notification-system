package com.notification.api.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 주기 실행을 활성화합니다.
 *
 * <p>outbox 발행 poller가 이 설정을 통해 동작합니다.
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(OutboxPublishProperties.class)
public class SchedulingConfig {
}
