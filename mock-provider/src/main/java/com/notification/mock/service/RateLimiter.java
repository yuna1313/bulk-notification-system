package com.notification.mock.service;

import java.time.Clock;
import org.springframework.stereotype.Component;

/**
 * mock-provider의 초당 요청 수 제한을 메모리에서 관리합니다.
 */
@Component
public class RateLimiter {

	private final Clock clock;
	private long windowSecond;
	private int requestCount;

	public RateLimiter() {
		this(Clock.systemUTC());
	}

	RateLimiter(Clock clock) {
		this.clock = clock;
	}

	/**
	 * 현재 1초 구간에서 요청을 허용할 수 있는지 확인하고 요청 수를 반영합니다.
	 *
	 * @param limitPerSecond 1초 동안 허용할 최대 요청 수
	 * @return 요청 한도 이내이면 true, 한도를 초과했으면 false
	 */
	public synchronized boolean tryAcquire(int limitPerSecond) {
		long currentSecond = clock.instant().getEpochSecond();
		if (currentSecond != windowSecond) {
			windowSecond = currentSecond;
			requestCount = 0;
		}

		if (requestCount >= limitPerSecond) {
			return false;
		}

		requestCount++;
		return true;
	}
}
