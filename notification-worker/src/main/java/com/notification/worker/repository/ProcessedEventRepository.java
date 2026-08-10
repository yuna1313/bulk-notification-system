package com.notification.worker.repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 이미 처리한 발송 지시를 기록해 중복 발송을 막습니다.
 *
 * <p>JPA 대신 JDBC를 쓰는 이유가 있습니다. Spring Data JPA의 {@code save()}는 식별자가 이미
 * 채워진 엔티티를 받으면 merge로 처리해서, 같은 키가 있으면 예외를 던지는 대신 조용히
 * update해버립니다. 그러면 두 번째 배달을 첫 번째와 구분할 수 없어 멱등성 판단에 쓸 수 없습니다.
 * 여기서 필요한 것은 "insert가 되는가 안 되는가"이므로 insert를 그대로 쓸 수 있는 JDBC를
 * 선택했습니다.
 */
@Repository
@RequiredArgsConstructor
public class ProcessedEventRepository {

	private final JdbcTemplate jdbcTemplate;

	/**
	 * 이 이벤트를 처음 처리하는 것이면 기록을 남기고 처리 권한을 가져옵니다.
	 *
	 * <p>기본키 충돌 여부로 판단하므로 판정과 기록이 한 번의 insert로 끝납니다. 조회한 뒤
	 * insert하면 그 사이에 다른 스레드가 끼어들 수 있지만, 이 방식은 DB가 유일성을 보장하므로
	 * 워커 스레드 10개가 같은 이벤트를 동시에 집어도 정확히 하나만 통과합니다.
	 *
	 * <p>트랜잭션을 걸지 않아 insert가 즉시 확정됩니다. 발송이 끝날 때까지 미뤄두면 그동안
	 * 다른 스레드가 같은 이벤트를 통과시킬 수 있습니다.
	 *
	 * @param eventId 이벤트 식별자
	 * @param messageId 발송 건 식별자
	 * @param processedAt 처리를 시작한 시각
	 * @return 처음 처리하는 것이면 true, 이미 처리한 이벤트면 false
	 */
	public boolean claim(String eventId, Long messageId, LocalDateTime processedAt) {
		try {
			jdbcTemplate.update(
					"insert into processed_event (event_id, message_id, processed_at) values (?, ?, ?)",
					eventId, messageId, Timestamp.valueOf(processedAt)
			);
			return true;
		} catch (DuplicateKeyException exception) {
			return false;
		}
	}
}
