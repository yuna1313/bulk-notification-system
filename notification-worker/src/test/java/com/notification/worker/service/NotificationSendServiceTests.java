package com.notification.worker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import com.notification.worker.client.MockProviderClient;
import com.notification.worker.client.ProviderSendResult;
import com.notification.worker.client.dto.ProviderSendRequest;
import com.notification.worker.domain.FailureReason;
import com.notification.worker.domain.MessageStatus;
import com.notification.worker.domain.NotificationMessage;
import com.notification.worker.event.NotificationSendEvent;
import com.notification.worker.repository.NotificationMessageRepository;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * 워커가 발송사 호출 결과를 발송 건에 기록하는지 검증합니다.
 *
 * <p>발송 건은 notification-api가 만드는 행이라 워커에는 생성 코드가 없습니다.
 * 그래서 테스트에서만 SQL로 직접 넣습니다.
 */
@SpringBootTest
class NotificationSendServiceTests {

	@Autowired
	private NotificationSendService sendService;

	@Autowired
	private NotificationMessageRepository messageRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@MockitoBean
	private MockProviderClient providerClient;

	@AfterEach
	void clear() {
		messageRepository.deleteAll();
		jdbcTemplate.update("delete from processed_event");
	}

	@Test
	void skipsWhenSameEventArrivesTwice() {
		Long messageId = insertPendingMessage();
		given(providerClient.send(any(ProviderSendRequest.class)))
				.willReturn(ProviderSendResult.success("provider-message-1"));
		NotificationSendEvent event = eventFor(messageId);

		sendService.send(event);
		sendService.send(event);

		then(providerClient).should(times(1)).send(any(ProviderSendRequest.class));
	}

	/**
	 * 구간 재처리는 같은 발송 건에 대해 이벤트 식별자를 새로 발급합니다.
	 * 중복 차단이 재처리까지 막아버리지 않는지 확인합니다.
	 */
	@Test
	void sendsAgainWhenSameMessageArrivesWithNewEventId() {
		Long messageId = insertPendingMessage();
		given(providerClient.send(any(ProviderSendRequest.class)))
				.willReturn(ProviderSendResult.success("provider-message-1"));

		sendService.send(eventFor(messageId));
		sendService.send(eventFor(messageId));

		then(providerClient).should(times(2)).send(any(ProviderSendRequest.class));
	}

	@Test
	void recordsSuccessWhenProviderAccepts() {
		Long messageId = insertPendingMessage();
		given(providerClient.send(any(ProviderSendRequest.class)))
				.willReturn(ProviderSendResult.success("provider-message-1"));

		sendService.send(eventFor(messageId));

		NotificationMessage message = messageRepository.findById(messageId).orElseThrow();
		assertThat(message.getStatus()).isEqualTo(MessageStatus.SUCCESS);
		assertThat(message.getProviderMessageId()).isEqualTo("provider-message-1");
		assertThat(message.getFailureReason()).isNull();
		assertThat(message.getSentAt()).isNotNull();
	}

	@Test
	void recordsFailureReasonWhenProviderRejects() {
		Long messageId = insertPendingMessage();
		given(providerClient.send(any(ProviderSendRequest.class)))
				.willReturn(ProviderSendResult.fail(FailureReason.RATE_LIMIT));

		sendService.send(eventFor(messageId));

		NotificationMessage message = messageRepository.findById(messageId).orElseThrow();
		assertThat(message.getStatus()).isEqualTo(MessageStatus.FAIL);
		assertThat(message.getFailureReason()).isEqualTo(FailureReason.RATE_LIMIT);
		assertThat(message.getProviderMessageId()).isNull();
		assertThat(message.getSentAt()).isNotNull();
	}

	@Test
	void sendsWhatTheEventCarriesWithoutReadingItFromDatabase() {
		Long messageId = insertPendingMessage();
		given(providerClient.send(any(ProviderSendRequest.class)))
				.willReturn(ProviderSendResult.success("provider-message-1"));

		sendService.send(eventFor(messageId));

		then(providerClient).should().send(new ProviderSendRequest(
				String.valueOf(messageId), "user-1", "SMS", "점검 안내입니다."));
	}

	@Test
	void skipsWhenMessageDoesNotExist() {
		sendService.send(eventFor(999_999L));

		then(providerClient).should(never()).send(any(ProviderSendRequest.class));
	}

	private NotificationSendEvent eventFor(Long messageId) {
		return new NotificationSendEvent(
				UUID.randomUUID().toString(), 1L, messageId, "user-1", "SMS", "점검 안내입니다.");
	}

	private Long insertPendingMessage() {
		jdbcTemplate.update(
				"insert into notification_message (status, updated_at) values (?, ?)",
				MessageStatus.PENDING.name(),
				Timestamp.valueOf(LocalDateTime.now())
		);
		return jdbcTemplate.queryForObject("select max(id) from notification_message", Long.class);
	}
}
