package com.notification.api.domain;

/**
 * 발송 요청 한 건의 진행 상태입니다.
 */
public enum NotificationStatus {

	/** 발송 요청이 접수되었고 아직 발송을 시작하지 않은 상태입니다. */
	PENDING,

	/** 수신자별 발송을 진행하고 있는 상태입니다. */
	DISPATCHING,

	/** 모든 수신자에 대한 발송 시도가 끝난 상태입니다. 일부 실패가 포함될 수 있습니다. */
	COMPLETED
}
