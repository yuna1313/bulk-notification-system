package com.notification.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 수신자 여러 명에게 같은 내용을 보내는 발송 요청 한 건입니다.
 *
 * <p>수신자별 발송 결과는 {@link NotificationMessage}가 건별로 보관합니다.
 */
@Entity
@Table(
		name = "notification",
		indexes = @Index(
				name = "idx_notification_status_scheduled_at",
				columnList = "status, scheduled_at"
		)
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	private String title;

	@Column(nullable = false, length = 1000)
	private String content;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private NotificationChannel channel;

	/**
	 * 발송하기로 예약한 시각입니다.
	 *
	 * <p>과거 시각도 허용하며, 이 경우 즉시 발송 대상으로 취급합니다.
	 */
	@Column(nullable = false)
	private LocalDateTime scheduledAt;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private NotificationStatus status;

	/** 발송을 시작한 시각입니다. 완료 시각과의 차이가 곧 발송 소요시간입니다. */
	private LocalDateTime dispatchStartedAt;

	/** 모든 수신자에 대한 발송 시도가 끝난 시각입니다. */
	private LocalDateTime dispatchFinishedAt;

	private Notification(String title, String content, NotificationChannel channel, LocalDateTime scheduledAt) {
		this.title = title;
		this.content = content;
		this.channel = channel;
		this.scheduledAt = scheduledAt;
		this.status = NotificationStatus.PENDING;
	}

	/**
	 * 발송 대기 상태의 발송 요청을 생성합니다.
	 *
	 * @param title 발송 요청을 구분하기 위한 제목
	 * @param content 수신자에게 전달할 내용
	 * @param channel 발송 수단
	 * @param scheduledAt 발송하기로 예약한 시각
	 * @return 발송 대기 상태의 발송 요청
	 */
	public static Notification create(
			String title,
			String content,
			NotificationChannel channel,
			LocalDateTime scheduledAt
	) {
		return new Notification(title, content, channel, scheduledAt);
	}

	/**
	 * 이 발송 요청에 속한 수신자별 발송 건을 만듭니다.
	 *
	 * @param recipientId 수신자 식별자
	 * @return 발송 대기 상태의 발송 건
	 */
	public NotificationMessage addMessage(String recipientId) {
		return NotificationMessage.create(this, recipientId);
	}

	/**
	 * 발송을 시작한 것으로 표시하고 시작 시각을 기록합니다.
	 *
	 * @param startedAt 발송을 시작한 시각
	 */
	public void startDispatch(LocalDateTime startedAt) {
		this.status = NotificationStatus.DISPATCHING;
		this.dispatchStartedAt = startedAt;
	}

	/**
	 * 발송이 끝난 것으로 표시하고 완료 시각을 기록합니다.
	 *
	 * @param finishedAt 모든 발송 시도가 끝난 시각
	 */
	public void finishDispatch(LocalDateTime finishedAt) {
		this.status = NotificationStatus.COMPLETED;
		this.dispatchFinishedAt = finishedAt;
	}

	/**
	 * 지금 발송을 시작할 수 있는 상태인지 확인합니다.
	 *
	 * @return 아직 발송을 시작하지 않았다면 true
	 */
	public boolean isDispatchable() {
		return this.status == NotificationStatus.PENDING;
	}
}
