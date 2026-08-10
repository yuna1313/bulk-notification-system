package com.notification.api.repository;

import com.notification.api.domain.MessageStatus;

/**
 * 발송 건을 상태별로 집계한 결과입니다.
 */
public interface MessageStatusCount {

	/**
	 * 집계 대상 상태를 반환합니다.
	 *
	 * @return 발송 건 상태
	 */
	MessageStatus getStatus();

	/**
	 * 해당 상태의 발송 건수를 반환합니다.
	 *
	 * @return 발송 건수
	 */
	long getCount();
}
