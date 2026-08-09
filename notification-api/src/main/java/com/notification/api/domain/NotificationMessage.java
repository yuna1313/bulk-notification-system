package com.notification.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 수신자 한 명에게 보내는 발송 건입니다.
 *
 * <p>외부 발송사 호출 한 번이 이 엔티티 한 건에 대응하며, 호출 결과를 건별로 기록합니다.
 */
@Entity
@Table(
		name = "notification_message",
		indexes = @Index(
				name = "idx_notification_message_notification_id_status",
				columnList = "notification_id, status"
		)
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationMessage extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "notification_id", nullable = false)
	private Notification notification;

	@Column(nullable = false, length = 50)
	private String recipientId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private MessageStatus status;

	@Enumerated(EnumType.STRING)
	@Column(length = 20)
	private FailureReason failureReason;

	/** 외부 발송사가 접수 후 돌려준 식별자입니다. */
	@Column(length = 64)
	private String providerMessageId;

	private LocalDateTime sentAt;

	private NotificationMessage(Notification notification, String recipientId) {
		this.notification = notification;
		this.recipientId = recipientId;
		this.status = MessageStatus.PENDING;
	}

	/**
	 * 발송 대기 상태의 발송 건을 생성합니다.
	 *
	 * @param notification 이 발송 건이 속한 발송 요청
	 * @param recipientId 수신자 식별자
	 * @return 발송 대기 상태의 발송 건
	 */
	static NotificationMessage create(Notification notification, String recipientId) {
		return new NotificationMessage(notification, recipientId);
	}

	/**
	 * 발송 성공으로 기록합니다.
	 *
	 * @param providerMessageId 외부 발송사가 돌려준 식별자
	 * @param sentAt 발송사 호출이 끝난 시각
	 */
	public void markSuccess(String providerMessageId, LocalDateTime sentAt) {
		this.status = MessageStatus.SUCCESS;
		this.providerMessageId = providerMessageId;
		this.sentAt = sentAt;
		this.failureReason = null;
	}

	/**
	 * 발송 실패로 기록합니다. v1은 재시도하지 않으므로 이 상태가 최종 결과입니다.
	 *
	 * @param failureReason 실패 사유
	 * @param sentAt 발송사 호출이 끝난 시각
	 */
	public void markFail(FailureReason failureReason, LocalDateTime sentAt) {
		this.status = MessageStatus.FAIL;
		this.failureReason = failureReason;
		this.sentAt = sentAt;
	}
}
