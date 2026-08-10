package com.notification.api.dto;

import com.notification.api.domain.NotificationChannel;
import com.notification.api.domain.NotificationStatus;
import java.time.LocalDateTime;

/**
 * 발송 요청의 현재 진행 현황을 표현합니다.
 *
 * @param notificationId 발송 요청 식별자
 * @param title 발송 요청 제목
 * @param channel 발송 수단
 * @param status 발송 요청 진행 상태
 * @param scheduledAt 예약 시각
 * @param dispatchStartedAt 발송 시작 시각. 아직 시작하지 않았다면 null입니다.
 * @param dispatchFinishedAt 발송 종료 시각. 아직 끝나지 않았다면 null입니다.
 * @param elapsedMillis 발송에 걸린 시간. 발송이 끝나야 값이 채워집니다.
 * @param totalCount 전체 발송 건수
 * @param successCount 발송에 성공한 건수
 * @param failCount 발송에 실패한 건수
 * @param pendingCount 아직 발송하지 않은 건수
 */
public record NotificationStatusResponse(
		Long notificationId,
		String title,
		NotificationChannel channel,
		NotificationStatus status,
		LocalDateTime scheduledAt,
		LocalDateTime dispatchStartedAt,
		LocalDateTime dispatchFinishedAt,
		Long elapsedMillis,
		long totalCount,
		long successCount,
		long failCount,
		long pendingCount
) {
}
