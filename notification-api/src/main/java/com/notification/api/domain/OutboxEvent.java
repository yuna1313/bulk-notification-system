package com.notification.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Kafka로 내보낼 이벤트를 발송 건과 같은 트랜잭션에 기록해두는 outbox 항목입니다.
 *
 * <p>발송 상태 변경을 DB에 커밋한 뒤 Kafka 발행을 따로 호출하면, 그 사이에 프로세스가 죽었을 때
 * 발송 지시가 사라집니다. 두 저장소에 걸친 원자성은 확보할 수 없으므로, 이벤트를 같은 DB
 * 트랜잭션에 함께 저장해두고 별도 poller가 이어서 발행합니다. 커밋에 성공한 이벤트는
 * 반드시 언젠가 발행되고, 트랜잭션이 실패하면 이벤트도 함께 사라집니다.
 *
 * <p>이 구조는 최소 한 번(at-least-once) 발행을 보장합니다. 발행 직후 상태를 바꾸기 전에
 * 죽으면 같은 이벤트가 두 번 나갈 수 있으므로, 중복 제거는 {@code eventId}를 받는
 * 워커 쪽 책임입니다.
 */
@Entity
@Table(
		name = "outbox_event",
		indexes = @Index(
				name = "idx_outbox_event_status_id",
				columnList = "status, id"
		),
		uniqueConstraints = @UniqueConstraint(
				name = "uk_outbox_event_event_id",
				columnNames = "event_id"
		)
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * 이벤트 식별자입니다. 워커가 중복 발송을 걸러내는 멱등성 키로 씁니다.
	 *
	 * <p>발송 대상을 가리키는 {@code messageId}로도 중복은 걸러집니다. 그럼에도 따로 두는 이유는
	 * 구간 재처리 때문입니다. 재처리는 같은 {@code messageId}에 대해 이벤트를 새로 만들어 다시
	 * 발행하는데, 워커가 {@code messageId}로 중복을 판단하면 재처리분까지 이미 처리한 건으로 보고
	 * 버립니다. 중복 차단 장치가 재처리를 막아버리는 것입니다.
	 *
	 * <p>이벤트마다 새 값을 발급하면 "같은 이벤트가 두 번 배달된 것"과 "의도해서 다시 발행한 것"이
	 * 구분됩니다. 앞의 것은 버리고 뒤의 것은 처리해야 합니다.
	 */
	@Column(name = "event_id", nullable = false, length = 36, updatable = false)
	private String eventId;

	/** 이 이벤트가 속한 발송 요청 식별자입니다. 나중에 구간 재처리에서 대상을 고르는 기준이 됩니다. */
	@Column(nullable = false, updatable = false)
	private Long notificationId;

	/** 수신자별 발송 건 식별자입니다. Kafka 파티션 키로 써서 같은 발송 건이 항상 같은 파티션으로 가게 합니다. */
	@Column(nullable = false, updatable = false)
	private Long messageId;

	/** 직렬화한 {@code NotificationSendEvent}입니다. 워커가 이 값만으로 발송할 수 있어야 합니다. */
	@Lob
	@Column(nullable = false, updatable = false)
	private String payload;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private OutboxStatus status;

	/** Kafka 발행을 마친 시각입니다. 접수부터 발행까지 걸린 시간을 재는 데 씁니다. */
	private LocalDateTime publishedAt;

	private OutboxEvent(String eventId, Long notificationId, Long messageId, String payload) {
		this.eventId = eventId;
		this.notificationId = notificationId;
		this.messageId = messageId;
		this.payload = payload;
		this.status = OutboxStatus.PENDING;
	}

	/**
	 * 발행 대기 상태의 outbox 항목을 생성합니다.
	 *
	 * @param notificationId 이 이벤트가 속한 발송 요청 식별자
	 * @param messageId 수신자별 발송 건 식별자
	 * @param payload 직렬화한 이벤트 본문
	 * @return 발행 대기 상태의 outbox 항목
	 */
	public static OutboxEvent create(Long notificationId, Long messageId, String payload) {
		return new OutboxEvent(UUID.randomUUID().toString(), notificationId, messageId, payload);
	}

	/**
	 * 발행을 마친 것으로 표시하고 발행 시각을 기록합니다.
	 *
	 * @param publishedAt Kafka가 발행을 확인해준 시각
	 */
	public void markPublished(LocalDateTime publishedAt) {
		this.status = OutboxStatus.PUBLISHED;
		this.publishedAt = publishedAt;
	}
}
