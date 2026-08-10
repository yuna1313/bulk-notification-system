package com.notification.api.repository;

import com.notification.api.domain.OutboxEvent;
import com.notification.api.domain.OutboxStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * outbox 항목을 조회하고 저장합니다.
 */
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

	/**
	 * 아직 발행하지 않은 이벤트를 쌓인 순서대로 조회합니다.
	 *
	 * <p>발행 순서를 보장하기 위해 {@code id} 오름차순으로 읽습니다. 다만 순서가 보장되는 범위는
	 * 같은 파티션 안까지이며, 파티션이 여러 개이므로 전체 발송 순서는 보장되지 않습니다.
	 *
	 * <p>행 잠금을 걸지 않습니다. API 인스턴스가 한 대라는 전제이며, 여러 대로 늘리면
	 * 같은 이벤트를 여러 인스턴스가 집어가 중복 발행됩니다.
	 *
	 * @param status 조회할 발행 상태
	 * @param limit 한 번에 가져올 최대 건수
	 * @return 발행 대기 이벤트 목록
	 */
	List<OutboxEvent> findByStatusOrderByIdAsc(OutboxStatus status, Limit limit);

	/**
	 * 발행을 마친 이벤트를 한 번의 update로 표시합니다.
	 *
	 * <p>엔티티를 하나씩 저장하면 500건 발행에 update가 500번 나갑니다. 발행 자체보다 이쪽이
	 * 더 오래 걸리므로 묶어서 처리합니다.
	 *
	 * <p>벌크 연산이라 영속성 컨텍스트를 거치지 않습니다. 따라서 {@code updatedAt}은 갱신되지
	 * 않으며, 발행 시각은 {@code publishedAt}으로 확인해야 합니다.
	 *
	 * @param ids 발행을 마친 outbox 항목 식별자 목록
	 * @param publishedAt 발행을 확인한 시각
	 * @return 상태를 바꾼 행 수
	 */
	@Transactional
	@Modifying(clearAutomatically = true)
	@Query("""
			update OutboxEvent o
			set o.status = com.notification.api.domain.OutboxStatus.PUBLISHED,
			    o.publishedAt = :publishedAt
			where o.id in :ids
			""")
	int markPublished(@Param("ids") List<Long> ids, @Param("publishedAt") LocalDateTime publishedAt);
}
