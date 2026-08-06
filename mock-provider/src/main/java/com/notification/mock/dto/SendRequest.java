package com.notification.mock.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 외부 발송사로 전달되는 발송 요청 정보를 표현합니다.
 */
@Getter
@NoArgsConstructor
public class SendRequest {

	private String messageId;
	private String recipientId;
	private String channel;
	private String content;
}
