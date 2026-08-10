package com.notification.worker.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 워커가 이미 처리한 발송 지시입니다. 중복 발송을 막는 유일한 근거입니다.
 *
 * <p>notification-api는 이 테이블을 쓰지 않습니다. 워커만 읽고 씁니다.
 *
 * <p>이 엔티티로 저장하지는 않습니다. 저장은
 * {@link com.notification.worker.repository.ProcessedEventRepository}가 JDBC로 처리하며,
 * 이유는 그쪽에 적어두었습니다. 여기 엔티티를 둔 것은 다른 테이블처럼 매핑에서 스키마가
 * 만들어지게 하기 위해서입니다. 이것이 없으면 이 테이블만 별도 DDL로 관리해야 합니다.
 */
@Entity
@Table(name = "processed_event")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProcessedEvent {

	/**
	 * 이벤트 식별자입니다.
	 *
	 * <p>기본키로 두어 중복 insert를 DB가 막게 합니다. 워커 스레드 여러 개가 같은 이벤트를
	 * 동시에 집어도 한 번만 통과합니다.
	 */
	@Id
	@Column(name = "event_id", length = 36)
	private String eventId;

	@Column(nullable = false)
	private Long messageId;

	/** 처리를 시작한 시각입니다. 발송을 마친 시각이 아닙니다. */
	@Column(nullable = false)
	private LocalDateTime processedAt;
}
