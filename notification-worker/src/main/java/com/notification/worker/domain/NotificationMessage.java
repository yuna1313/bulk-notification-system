package com.notification.worker.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 수신자 한 명에게 보내는 발송 건입니다.
 *
 * <p>이 행을 만드는 것은 notification-api이고, 워커는 발송 결과만 채워 넣습니다.
 * 그래서 행을 생성하는 메서드가 없습니다.
 *
 * <p>테이블 전체가 아니라 <b>워커가 실제로 쓰는 컬럼만</b> 매핑했습니다. 수신자와 내용은
 * 이벤트에 실려 오므로 DB에서 읽을 필요가 없고, 읽지 않는 컬럼까지 매핑하면 한쪽을 고칠 때
 * 양쪽이 어긋날 여지만 늘어납니다. 매핑하지 않은 컬럼은 이 프로그램이 건드리지 않습니다.
 */
@Entity
@Table(name = "notification_message")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationMessage {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

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

	/**
	 * 마지막으로 수정된 시각입니다.
	 *
	 * <p>notification-api는 JPA Auditing으로 자동 갱신하지만, 워커는 이 컬럼 하나 때문에
	 * Auditing 설정을 두지 않고 발송 결과를 기록할 때 직접 채웁니다.
	 */
	@Column(nullable = false)
	private LocalDateTime updatedAt;

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
		this.updatedAt = sentAt;
	}

	/**
	 * 발송 실패로 기록합니다.
	 *
	 * @param failureReason 실패 사유
	 * @param sentAt 발송사 호출이 끝난 시각
	 */
	public void markFail(FailureReason failureReason, LocalDateTime sentAt) {
		this.status = MessageStatus.FAIL;
		this.failureReason = failureReason;
		this.sentAt = sentAt;
		this.updatedAt = sentAt;
	}
}
