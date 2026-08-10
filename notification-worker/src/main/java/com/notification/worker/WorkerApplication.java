package com.notification.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 발송 지시를 Kafka에서 받아 처리하는 워커입니다.
 *
 * <p>발송 요청 접수와 현황 조회는 notification-api가 담당하고, 이 프로세스는 발송만 합니다.
 * 발송이 느려져도 접수 API가 영향을 받지 않게 하는 것이 분리한 이유입니다.
 */
@SpringBootApplication
public class WorkerApplication {

	public static void main(String[] args) {
		SpringApplication.run(WorkerApplication.class, args);
	}
}
