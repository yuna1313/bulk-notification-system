package com.notification.worker.event;

/**
 * notification-api가 보낸 발송 지시입니다.
 *
 * <p>notification-api의 같은 이름 레코드와 필드가 일치해야 합니다. 두 프로젝트가 독립적이라
 * 컴파일러가 이 일치를 검사해주지 못합니다. 한쪽 필드를 바꾸면 역직렬화가 조용히 어긋나므로,
 * 필드를 손볼 때는 반드시 양쪽을 함께 고쳐야 합니다.
 *
 * @param eventId 이벤트 식별자. 같은 이벤트가 두 번 배달됐는지 판단하는 기준입니다
 * @param notificationId 이 발송 건이 속한 발송 요청 식별자
 * @param messageId 수신자별 발송 건 식별자
 * @param recipientId 수신자 식별자
 * @param channel 발송 수단
 * @param content 수신자에게 전달할 내용
 */
public record NotificationSendEvent(
		String eventId,
		Long notificationId,
		Long messageId,
		String recipientId,
		String channel,
		String content
) {
}
