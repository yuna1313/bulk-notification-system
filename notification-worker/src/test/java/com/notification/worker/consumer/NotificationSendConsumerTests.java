package com.notification.worker.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.notification.worker.event.NotificationSendEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.ObjectMapper;

/**
 * notification-api가 보낸 형식을 워커가 그대로 읽어낼 수 있는지 검증합니다.
 *
 * <p>두 프로젝트가 독립적이라 이벤트 정의가 어긋나도 컴파일러가 잡아주지 못합니다.
 * 아래 JSON은 notification-api가 실제로 만들어 넣는 형태이며, 한쪽 필드가 바뀌면
 * 이 테스트가 먼저 깨지도록 두었습니다.
 */
@SpringBootTest
class NotificationSendConsumerTests {

	private static final String PAYLOAD = """
			{"eventId":"3f2a9c11-5d77-4f0e-9d5c-2b1a7c6e4d80",\
			"notificationId":42,"messageId":1001,"recipientId":"user-1",\
			"channel":"SMS","content":"8월 10일 02시부터 04시까지 점검이 진행됩니다."}\
			""";

	@Autowired
	private NotificationSendConsumer consumer;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void readsEveryFieldOfPayloadProducedByApi() {
		NotificationSendEvent event = objectMapper.readValue(PAYLOAD, NotificationSendEvent.class);

		assertThat(event.eventId()).isEqualTo("3f2a9c11-5d77-4f0e-9d5c-2b1a7c6e4d80");
		assertThat(event.notificationId()).isEqualTo(42L);
		assertThat(event.messageId()).isEqualTo(1001L);
		assertThat(event.recipientId()).isEqualTo("user-1");
		assertThat(event.channel()).isEqualTo("SMS");
		assertThat(event.content()).isEqualTo("8월 10일 02시부터 04시까지 점검이 진행됩니다.");
	}

	@Test
	void consumesRecordWithoutError() {
		ConsumerRecord<String, String> record =
				new ConsumerRecord<>("notification.send", 3, 7L, "1001", PAYLOAD);

		assertThatCode(() -> consumer.consume(record)).doesNotThrowAnyException();
	}
}
