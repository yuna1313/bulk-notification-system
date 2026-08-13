package com.notification.worker.service;

import com.notification.worker.domain.FailureReason;
import lombok.Getter;

/**
 * 발송사 호출이 실패했음을 컨슈머 밖으로 알립니다.
 *
 * <p>이 예외가 리스너 밖으로 나가야 Kafka가 같은 메시지를 다시 배달합니다. 발송 실패를
 * 조용히 삼키면 오프셋이 커밋되어 그 건은 영영 다시 시도되지 않습니다. v1에서 실패 2,974건이
 * 그대로 유실됐던 것이 그 구조였습니다.
 */
@Getter
public class NotificationSendFailedException extends RuntimeException {

	private final Long messageId;
	private final FailureReason failureReason;

	public NotificationSendFailedException(Long messageId, FailureReason failureReason) {
		super("발송에 실패했습니다. messageId=%d, failureReason=%s".formatted(messageId, failureReason));
		this.messageId = messageId;
		this.failureReason = failureReason;
	}
}
