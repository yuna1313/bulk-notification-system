package com.notification.api.repository;

import com.notification.api.domain.NotificationMessage;
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
}
