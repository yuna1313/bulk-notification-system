package com.notification.api.scheduler;

import com.notification.api.service.OutboxPublishService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * outbox 발행을 주기적으로 실행합니다.
 *
 * <p>{@code fixedDelay}이므로 이전 주기가 끝난 뒤부터 간격을 셉니다. 발행이 오래 걸려도
 * 다음 주기가 겹쳐 들어오지 않습니다. 같은 이벤트를 두 주기가 동시에 집어가는 것을 막기 위해
 * 조회에 잠금을 걸지 않았으므로, 겹치지 않는 것이 전제입니다.
 *
 * <p>같은 이유로 API 인스턴스를 여러 대로 늘리면 인스턴스끼리 같은 이벤트를 집어가 중복
 * 발행합니다. 지금은 한 대로 전제하고 있습니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
		prefix = "notification.outbox.publish",
		name = "enabled",
		havingValue = "true",
		matchIfMissing = true
)
public class OutboxPublishScheduler {

	private final OutboxPublishService publishService;

	/**
	 * 발행 대기 이벤트를 한 묶음 발행합니다.
	 *
	 * <p>예외가 스케줄러 밖으로 나가도 다음 주기는 계속 실행되지만, 무슨 일이 있었는지
	 * 남기기 위해 여기서 잡습니다.
	 */
	@Scheduled(fixedDelayString = "${notification.outbox.publish.interval-millis}")
	public void publish() {
		try {
			publishService.publishPending();
		} catch (Exception e) {
			log.error("[outbox] 발행 주기 실행 중 예외가 발생했습니다.", e);
		}
	}
}
