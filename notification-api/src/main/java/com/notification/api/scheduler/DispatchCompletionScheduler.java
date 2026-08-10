package com.notification.api.scheduler;

import com.notification.api.service.DispatchCompletionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 발송 완료 판정을 주기적으로 실행합니다.
 *
 * <p>워커가 발송을 끝내도 발송 요청의 상태는 그대로 남아 있습니다. 이 주기 실행이 그 상태를
 * 완료로 옮기는 유일한 경로입니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
		prefix = "notification.dispatch.completion",
		name = "enabled",
		havingValue = "true",
		matchIfMissing = true
)
public class DispatchCompletionScheduler {

	private final DispatchCompletionService completionService;

	@Scheduled(fixedDelayString = "${notification.dispatch.completion.interval-millis}")
	public void complete() {
		try {
			completionService.completeFinishedDispatches();
		} catch (Exception e) {
			log.error("[completion] 발송 완료 판정 중 예외가 발생했습니다.", e);
		}
	}
}
