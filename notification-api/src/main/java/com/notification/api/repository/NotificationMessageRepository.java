package com.notification.api.repository;

import com.notification.api.domain.NotificationMessage;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 발송 건을 조회하고 저장합니다.
 */
public interface NotificationMessageRepository extends JpaRepository<NotificationMessage, Long> {

	/**
	 * 발송 요청에 속한 발송 건을 모두 조회합니다.
	 *
	 * @param notificationId 발송 요청 식별자
	 * @return 발송 건 목록
	 */
	List<NotificationMessage> findAllByNotificationId(Long notificationId);

	/**
	 * 발송 요청에 속한 발송 건을 상태별로 집계합니다.
	 *
	 * <p>성공, 실패 건수를 별도 컬럼에 누적해두지 않고 매번 집계합니다.
	 *
	 * @param notificationId 발송 요청 식별자
	 * @return 상태별 발송 건수 목록
	 */
	@Query("""
			select m.status as status, count(m) as count
			from NotificationMessage m
			where m.notification.id = :notificationId
			group by m.status
			""")
	List<MessageStatusCount> countGroupByStatus(@Param("notificationId") Long notificationId);

	/**
	 * 발송 요청에 속한 발송 건 중 가장 마지막에 발송된 시각을 조회합니다.
	 *
	 * <p>완료 시각을 판정 시각이 아니라 실제 마지막 발송 시각으로 남기기 위해 씁니다.
	 * 판정은 주기 실행이라 실제 완료보다 늦게 일어나며, 그 지연이 발송 소요시간에 섞이면
	 * 처리량 측정값이 틀어집니다.
	 *
	 * @param notificationId 발송 요청 식별자
	 * @return 마지막 발송 시각. 발송된 건이 하나도 없으면 null
	 */
	@Query("select max(m.sentAt) from NotificationMessage m where m.notification.id = :notificationId")
	LocalDateTime findLastSentAt(@Param("notificationId") Long notificationId);
}
