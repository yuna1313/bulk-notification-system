package com.notification.api.config;

import java.time.Duration;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.apache.kafka.clients.admin.NewTopic;

/**
 * 발송에 사용할 Kafka 토픽을 정의합니다.
 *
 * <p>브로커의 자동 토픽 생성을 꺼두었기 때문에 이 설정이 토픽을 만드는 유일한 경로입니다.
 * 애플리케이션 기동 시 토픽이 없으면 생성하고, 이미 있으면 그대로 둡니다.
 *
 * <p>이미 만들어진 토픽의 파티션 수는 이 설정을 바꿔도 줄어들지 않습니다.
 * 파티션 수를 바꿔 측정할 때는 토픽을 지우고 다시 만들어야 합니다.
 */
@Configuration
@EnableConfigurationProperties(KafkaTopicProperties.class)
public class KafkaTopicConfig {

	/**
	 * 수신자별 발송 건을 전달할 토픽을 정의합니다.
	 *
	 * @param properties 토픽 설정
	 * @return 발송 토픽 정의
	 */
	@Bean
	public NewTopic notificationSendTopic(KafkaTopicProperties properties) {
		return TopicBuilder.name(properties.name())
				.partitions(properties.partitions())
				.replicas(properties.replicas())
				.config(
						TopicConfig.RETENTION_MS_CONFIG,
						String.valueOf(Duration.ofDays(properties.retentionDays()).toMillis())
				)
				.build();
	}
}
