package com.notification.worker.consumer;

import com.notification.worker.event.NotificationSendEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * 발송 토픽에서 발송 지시를 받습니다.
 *
 * <p>지금은 받은 것을 기록만 합니다. 발송사 호출과 결과 저장은 다음 단계에서 붙입니다.
 * 컨슈머 설정이 제대로 됐는지를 발송 로직과 분리해서 확인하기 위해서입니다.
 *
 * <p>메서드가 예외 없이 끝나야 오프셋이 커밋됩니다. 예외가 나가면 같은 메시지를 다시 받으므로
 * 처리에 실패한 발송이 조용히 사라지지는 않습니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationSendConsumer {

	private final ObjectMapper objectMapper;

	/**
	 * 발송 지시 한 건을 처리합니다.
	 *
	 * @param record 파티션과 오프셋을 함께 보기 위해 원본 레코드로 받습니다
	 */
	@KafkaListener(topics = "${notification.kafka.send-topic.name}")
	public void consume(ConsumerRecord<String, String> record) {
		NotificationSendEvent event = objectMapper.readValue(record.value(), NotificationSendEvent.class);
		log.info("[consume] 발송 지시 수신: partition={}, offset={}, key={}, eventId={}, messageId={}, recipientId={}",
				record.partition(), record.offset(), record.key(),
				event.eventId(), event.messageId(), event.recipientId());
	}
}
