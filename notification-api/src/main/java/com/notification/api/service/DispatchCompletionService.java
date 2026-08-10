package com.notification.api.service;

import com.notification.api.config.DispatchCompletionProperties;
import com.notification.api.domain.Notification;
import com.notification.api.repository.NotificationMessageRepository;
import com.notification.api.repository.NotificationRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 모든 발송 시도가 끝난 발송 요청을 완료 상태로 바꿉니다.
 *
 * <p>v1은 발송 for 문이 끝난 자리에서 바로 완료 처리를 했습니다. v2는 발송이 워커에서
 * 일어나므로 그런 자리가 없습니다. 그렇다고 워커가 완료 처리를 하면 발송 한 건이 끝날 때마다
 * "이 요청의 남은 건수가 0인가"를 물어야 해서, 10만 건 발송에 집계 쿼리가 10만 번 나갑니다.
 * 게다가 워커가 notification 테이블까지 쓰게 됩니다.
 *
 * <p>그래서 주기적으로 훑어 판정합니다. 대가는 발송이 실제로 끝난 시점과 상태가 바뀌는 시점
 * 사이에 최대 한 주기만큼 지연이 생긴다는 것입니다. 다만 완료 시각은 판정 시각이 아니라
 * 마지막 발송 시각으로 남기므로 발송 소요시간 측정에는 이 지연이 섞이지 않습니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DispatchCompletionService {

	private final NotificationRepository notificationRepository;
	private final NotificationMessageRepository messageRepository;
	private final DispatchCompletionProperties properties;

	/**
	 * 발송 중인 요청 중 남은 발송 건이 없는 것을 찾아 완료로 표시합니다.
	 *
	 * <p>일부가 실패로 끝났어도 완료로 봅니다. 완료는 "모든 발송 시도가 끝났다"는 뜻이지
	 * "모두 성공했다"는 뜻이 아닙니다. 성공과 실패 건수는 현황 조회에서 따로 확인합니다.
	 *
	 * @return 완료로 바꾼 발송 요청 수
	 */
	@Transactional
	public int completeFinishedDispatches() {
		List<Notification> completable = notificationRepository.findCompletableDispatches(
				Limit.of(properties.batchSize()));

		for (Notification notification : completable) {
			LocalDateTime finishedAt = messageRepository.findLastSentAt(notification.getId());
			notification.finishDispatch(finishedAt);
			log.info("[completion] 발송 완료: notificationId={}, finishedAt={}",
					notification.getId(), finishedAt);
		}
		return completable.size();
	}
}
