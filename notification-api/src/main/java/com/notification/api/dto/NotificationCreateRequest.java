package com.notification.api.dto;

import com.notification.api.domain.NotificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 발송 요청 접수에 필요한 정보를 표현합니다.
 */
@Getter
@NoArgsConstructor
public class NotificationCreateRequest {

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

	@NotEmpty(message = "수신자는 한 명 이상이어야 합니다.")
	private List<@NotBlank(message = "수신자 식별자는 비어 있을 수 없습니다.")
			@Size(max = 50, message = "수신자 식별자는 50자를 넘을 수 없습니다.") String> recipientIds;
}
