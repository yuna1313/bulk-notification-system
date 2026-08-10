package com.notification.worker.domain;

/**
 * 수신자 한 명에 대한 발송 건의 상태입니다.
 *
 * <p>notification-api의 같은 이름 enum과 값이 일치해야 합니다.
 */
public enum MessageStatus {

	/** 아직 발송을 시도하지 않은 상태입니다. */
	PENDING,

	/** 외부 발송사가 접수에 성공한 상태입니다. */
	SUCCESS,

	/** 외부 발송사 호출이 실패한 상태입니다. */
	FAIL
}
