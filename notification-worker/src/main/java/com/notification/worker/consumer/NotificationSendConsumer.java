package com.notification.worker.consumer;

import com.notification.worker.event.NotificationSendEvent;
import com.notification.worker.service.NotificationSendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * 발송 토픽에서 발송 지시를 받아 발송 처리로 넘깁니다.
 *
 * <p>메서드가 예외 없이 끝나야 오프셋이 커밋됩니다. 예외가 나가면 같은 메시지를 다시 받으므로
 * 처리에 실패한 발송이 조용히 사라지지는 않습니다. 다만 계속 실패하는 메시지는 계속 다시
 * 배달되어 뒤에 있는 메시지를 막게 되는데, 이 문제는 재시도와 DLQ 단계에서 다룹니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationSendConsumer {

	private final ObjectMapper objectMapper;
	private final NotificationSendService sendService;

	/**
	 * 발송 지시 한 건을 처리합니다.
	 *
	 * @param record 파티션과 오프셋을 함께 보기 위해 원본 레코드로 받습니다
	 */
	@KafkaListener(topics = "${notification.kafka.send-topic.name}")
	public void consume(ConsumerRecord<String, String> record) {
		NotificationSendEvent event = objectMapper.readValue(record.value(), NotificationSendEvent.class);
		log.debug("[consume] 발송 지시 수신: partition={}, offset={}, eventId={}, messageId={}",
				record.partition(), record.offset(), event.eventId(), event.messageId());

		sendService.send(event);
	}
}
