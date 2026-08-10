package com.notification.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.notification.api.domain.FailureReason;
import com.notification.api.domain.Notification;
import com.notification.api.domain.NotificationChannel;
import com.notification.api.domain.NotificationMessage;
import com.notification.api.domain.NotificationStatus;
import com.notification.api.repository.NotificationMessageRepository;
import com.notification.api.repository.NotificationRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 모든 발송 시도가 끝난 발송 요청을 완료로 바꾸는지 검증합니다.
 *
 * <p>v2는 발송이 워커에서 일어나 완료 시점을 붙잡을 자리가 없으므로, 주기적으로 훑어
 * 판정합니다. 그 판정이 언제 완료로 보고 언제 보지 않는지가 검증 대상입니다.
 */
@SpringBootTest
class DispatchCompletionServiceTests {

	private static final LocalDateTime FIRST_SENT_AT = LocalDateTime.of(2026, 8, 11, 10, 0, 0);
	private static final LocalDateTime LAST_SENT_AT = LocalDateTime.of(2026, 8, 11, 10, 0, 5);

	@Autowired
	private DispatchCompletionService completionService;

	@Autowired
	private NotificationRepository notificationRepository;

	@Autowired
	private NotificationMessageRepository messageRepository;

	@AfterEach
	void clear() {
		messageRepository.deleteAll();
		notificationRepository.deleteAll();
	}

	@Test
	void completesWhenNoMessageIsPending() {
		Notification notification = saveDispatching();
		saveSuccessMessage(notification, "user-1", FIRST_SENT_AT);
		saveFailMessage(notification, "user-2", LAST_SENT_AT);

		int completedCount = completionService.completeFinishedDispatches();

		assertThat(completedCount).isEqualTo(1);
		Notification found = notificationRepository.findById(notification.getId()).orElseThrow();
		assertThat(found.getStatus()).isEqualTo(NotificationStatus.COMPLETED);
	}

	/**
	 * 완료 시각은 판정이 돌아간 시각이 아니라 마지막 발송 시각이어야 합니다.
	 * 주기 실행 지연이 발송 소요시간에 섞이면 처리량 측정값이 틀어집니다.
	 */
	@Test
	void recordsLastSentAtAsFinishedAt() {
		Notification notification = saveDispatching();
		saveSuccessMessage(notification, "user-1", FIRST_SENT_AT);
		saveSuccessMessage(notification, "user-2", LAST_SENT_AT);

		completionService.completeFinishedDispatches();

		Notification found = notificationRepository.findById(notification.getId()).orElseThrow();
		assertThat(found.getDispatchFinishedAt()).isEqualTo(LAST_SENT_AT);
	}

	@Test
	void leavesDispatchingWhenSomeMessageIsStillPending() {
		Notification notification = saveDispatching();
		saveSuccessMessage(notification, "user-1", FIRST_SENT_AT);
		messageRepository.save(notification.addMessage("user-2"));

		int completedCount = completionService.completeFinishedDispatches();

		assertThat(completedCount).isZero();
		Notification found = notificationRepository.findById(notification.getId()).orElseThrow();
		assertThat(found.getStatus()).isEqualTo(NotificationStatus.DISPATCHING);
		assertThat(found.getDispatchFinishedAt()).isNull();
	}

	@Test
	void ignoresNotificationThatHasNotStartedDispatching() {
		Notification notification = notificationRepository.save(newNotification());
		messageRepository.save(notification.addMessage("user-1"));

		int completedCount = completionService.completeFinishedDispatches();

		assertThat(completedCount).isZero();
		Notification found = notificationRepository.findById(notification.getId()).orElseThrow();
		assertThat(found.getStatus()).isEqualTo(NotificationStatus.PENDING);
	}

	private Notification saveDispatching() {
		Notification notification = notificationRepository.save(newNotification());
		notification.startDispatch(FIRST_SENT_AT);
		return notificationRepository.save(notification);
	}

	private Notification newNotification() {
		return Notification.create(
				"8월 정기 점검 안내",
				"8월 10일 02시부터 04시까지 점검이 진행됩니다.",
				NotificationChannel.SMS,
				LocalDateTime.now().plusHours(1)
		);
	}

	private void saveSuccessMessage(Notification notification, String recipientId, LocalDateTime sentAt) {
		NotificationMessage message = messageRepository.save(notification.addMessage(recipientId));
		message.markSuccess("provider-message-1", sentAt);
		messageRepository.save(message);
	}

	private void saveFailMessage(Notification notification, String recipientId, LocalDateTime sentAt) {
		NotificationMessage message = messageRepository.save(notification.addMessage(recipientId));
		message.markFail(FailureReason.RATE_LIMIT, sentAt);
		messageRepository.save(message);
	}
}
