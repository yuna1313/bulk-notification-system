package com.notification.worker.config;

import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;
import tools.jackson.core.JacksonException;

/**
 * 발송 실패를 어떻게 다시 시도하고, 끝내 실패하면 어디로 보낼지 설정합니다.
 *
 * <p>컨슈머가 예외를 밖으로 던지면 Kafka는 오프셋을 커밋하지 않고 같은 메시지를 다시
 * 배달합니다. 이 설정은 그 재배달을 몇 번까지 할지, 소진되면 어디로 치울지를 정합니다.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties({DlqTopicProperties.class, RetryProperties.class})
public class KafkaConsumerConfig {

	/**
	 * 재시도를 소진한 메시지를 모아둘 토픽을 정의합니다.
	 *
	 * <p>브로커의 자동 토픽 생성이 꺼져 있어 이 설정이 없으면 DLQ로 보내는 순간 실패합니다.
	 *
	 * @param properties DLQ 토픽 설정
	 * @return DLQ 토픽 정의
	 */
	@Bean
	public NewTopic notificationSendDlqTopic(DlqTopicProperties properties) {
		return TopicBuilder.name(properties.name())
				.partitions(properties.partitions())
				.replicas(properties.replicas())
				.config(
						TopicConfig.RETENTION_MS_CONFIG,
						String.valueOf(Duration.ofDays(properties.retentionDays()).toMillis())
				)
				.build();
	}

	/**
	 * 리스너에서 예외가 나갔을 때의 처리를 정의합니다.
	 *
	 * <p>정해진 횟수만큼 다시 배달해보고, 그래도 실패하면 DLQ 토픽으로 옮긴 뒤 오프셋을
	 * 커밋합니다. 옮기지 않고 계속 재시도하면 그 메시지 하나가 같은 파티션의 뒤쪽 메시지를
	 * 영원히 막습니다.
	 *
	 * <p>파티션 번호를 {@code -1}로 두어 DLQ의 어느 파티션에 넣을지는 Kafka가 정하게 합니다.
	 * 원본 파티션 번호를 그대로 쓰면 DLQ 토픽의 파티션 수가 더 적을 때 발행이 실패합니다.
	 *
	 * <p>본문을 읽지 못한 경우는 다시 배달해도 똑같이 실패하므로 재시도 없이 바로 DLQ로
	 * 보냅니다.
	 *
	 * @param kafkaTemplate DLQ 발행에 쓸 프로듀서
	 * @param dlqTopicProperties DLQ 토픽 설정
	 * @param retryProperties 재시도 설정
	 * @return 리스너 공통 예외 처리기
	 */
	@Bean
	public DefaultErrorHandler kafkaErrorHandler(
			KafkaTemplate<String, String> kafkaTemplate,
			DlqTopicProperties dlqTopicProperties,
			RetryProperties retryProperties
	) {
		DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
				kafkaTemplate,
				(record, exception) -> new TopicPartition(dlqTopicProperties.name(), -1)
		);

		DefaultErrorHandler errorHandler = new DefaultErrorHandler(
				recoverer,
				new FixedBackOff(retryProperties.intervalMillis(), retryProperties.maxRetries())
		);
		errorHandler.addNotRetryableExceptions(JacksonException.class);
		errorHandler.setRetryListeners((record, exception, deliveryAttempt) ->
				log.warn("[retry] 발송 재시도: attempt={}, partition={}, offset={}, cause={}",
						deliveryAttempt, record.partition(), record.offset(), exception.getMessage()));

		return errorHandler;
	}
}
