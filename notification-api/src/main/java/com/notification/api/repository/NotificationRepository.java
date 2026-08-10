package com.notification.api.repository;

import com.notification.api.domain.Notification;
import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * 발송 요청을 조회하고 저장합니다.
 */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

	/**
	 * 발송 중인 요청 중 남은 발송 건이 없는 것을 오래된 순으로 조회합니다.
	 *
	 * <p>거르는 일을 DB에 맡기는 것이 중요합니다. 발송 중인 요청을 그대로 가져와 하나씩
	 * 확인하면, 영영 끝나지 않는 요청이 앞에 끼었을 때 매 주기마다 같은 것들만 집어오고
	 * 뒤에 있는 새 요청에는 도달하지 못합니다.
	 *
	 * @param limit 한 번에 가져올 최대 건수
	 * @return 완료로 바꿀 수 있는 발송 요청 목록
	 */
	@Query("""
			select n from Notification n
			where n.status = com.notification.api.domain.NotificationStatus.DISPATCHING
			and not exists (
				select 1 from NotificationMessage m
				where m.notification = n
				and m.status = com.notification.api.domain.MessageStatus.PENDING
			)
			order by n.id asc
			""")
	List<Notification> findCompletableDispatches(Limit limit);
}
