package com.notification.api.domain;

/**
 * 발송 건이 실패한 사유입니다.
 *
 * <p>부하 테스트에서 실패율 설정 때문에 실패한 것인지, 초당 요청 한도에 걸린 것인지를
 * 구분해야 하므로 실패 건에는 반드시 사유를 남깁니다.
 */
public enum FailureReason {

	/** 외부 발송사가 발송 실패로 응답한 경우입니다. */
	PROVIDER_FAIL,

	/** 외부 발송사의 초당 요청 한도를 초과한 경우입니다. */
	RATE_LIMIT,

	/** 외부 발송사 호출이 응답 시간을 넘겨 끊긴 경우입니다. */
	TIMEOUT,

	/** 위 어디에도 해당하지 않는 경우입니다. */
	UNKNOWN
}
