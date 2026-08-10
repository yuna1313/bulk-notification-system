// 발송 요청 접수 API의 응답시간 분포를 측정합니다. (측정 D)
//
// 실행 예시
//   k6 run load-test/scripts/create-api.js
//   k6 run -e RECIPIENT_COUNT=1000 -e VUS=20 -e DURATION=1m load-test/scripts/create-api.js
import http from 'k6/http';
import { check } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const RECIPIENT_COUNT = Number(__ENV.RECIPIENT_COUNT || 100);

export const options = {
	vus: Number(__ENV.VUS || 10),
	duration: __ENV.DURATION || '30s',
	thresholds: {
		http_req_failed: ['rate<0.01'],
	},
	summaryTrendStats: ['avg', 'min', 'med', 'p(50)', 'p(95)', 'p(99)', 'max'],
};

export default function () {
	const payload = JSON.stringify({
		title: '부하 테스트',
		content: '부하 테스트용 발송 요청입니다.',
		channel: 'SMS',
		scheduledAt: '2026-08-10T02:00:00',
		recipientCount: RECIPIENT_COUNT,
	});

	const response = http.post(`${BASE_URL}/api/test/notifications`, payload, {
		headers: { 'Content-Type': 'application/json' },
		timeout: '120s',
	});

	check(response, {
		'접수 성공(201)': (r) => r.status === 201,
	});
}
