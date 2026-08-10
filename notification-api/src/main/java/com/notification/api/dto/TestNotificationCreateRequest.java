package com.notification.api.dto;

import com.notification.api.domain.NotificationChannel;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 부하 테스트용 발송 요청 접수에 필요한 정보를 표현합니다.
 *
 * <p>수신자 목록 대신 수신자 수만 받아 서버가 식별자를 만들어 냅니다.
 */
@Getter
@NoArgsConstructor
public class TestNotificationCreateRequest {

	@NotBlank(message = "제목은 필수입니다.")
	@Size(max = 100, message = "제목은 100자를 넘을 수 없습니다.")
	private String title;

	@NotBlank(message = "내용은 필수입니다.")
	@Size(max = 1000, message = "내용은 1000자를 넘을 수 없습니다.")
	private String content;

	@NotNull(message = "발송 수단은 필수입니다.")
	private NotificationChannel channel;

	/** 과거 시각도 허용하며 이 경우 즉시 발송 대상으로 취급합니다. 값이 없으면 거부합니다. */
	@NotNull(message = "예약 시각은 필수입니다.")
	private LocalDateTime scheduledAt;

	@NotNull(message = "수신자 수는 필수입니다.")
	@Min(value = 1, message = "수신자 수는 1 이상이어야 합니다.")
	@Max(value = 1_000_000, message = "수신자 수는 1000000을 넘을 수 없습니다.")
	private Integer recipientCount;
}
