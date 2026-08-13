// 수신자 규모별 발송 소요시간을 측정합니다. (측정 A·B)
//
// v1은 POST /dispatch가 발송을 다 끝낸 뒤에 응답했으므로 curl 한 번이면 잴 수 있었습니다.
// v2는 발송 지시를 쌓기만 하고 즉시 202로 응답합니다. 그 응답시간은 접수에 걸린 시간일 뿐이며
// 발송 소요시간이 아닙니다. 발송이 끝났는지는 현황 조회를 폴링해서 확인해야 합니다.
//
// 발송 소요시간은 현황 조회 응답의 elapsedMillis를 그대로 씁니다. 이 값은 발송 시작 시각부터
// 마지막 발송 시각(max(sent_at))까지이므로, 완료 판정 주기(2초)나 아래 폴링 간격이 섞이지
// 않습니다. v1의 elapsedMillis와 정의가 같아 두 구조를 같은 선상에서 비교할 수 있습니다.
//
// 실행 예시
//   k6 run load-test/scripts/dispatch-duration.js
//   k6 run -e RECIPIENT_COUNT=10000 load-test/scripts/dispatch-duration.js
//   k6 run -e RECIPIENT_COUNT=100000 -e MAX_WAIT_MINUTES=120 -e POLL_INTERVAL_SECONDS=10 \
//     load-test/scripts/dispatch-duration.js
import http from 'k6/http';
import { sleep } from 'k6';
import { Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const RECIPIENT_COUNT = Number(__ENV.RECIPIENT_COUNT || 100);
const POLL_INTERVAL_SECONDS = Number(__ENV.POLL_INTERVAL_SECONDS || 5);
const MAX_WAIT_MINUTES = Number(__ENV.MAX_WAIT_MINUTES || 30);

const JSON_HEADERS = { headers: { 'Content-Type': 'application/json' } };

// 발송 시작부터 마지막 발송까지입니다. v1과 비교할 핵심 지표입니다.
const dispatchDuration = new Trend('dispatch_duration_ms', true);

// 발송 지시를 outbox에 쌓는 데 걸린 시간입니다. v2에서 사용자가 실제로 기다리는 시간입니다.
const queueDuration = new Trend('queue_duration_ms', true);

// 초당 발송 건수입니다. 규모를 바꿔가며 이 값이 유지되는지 보는 것이 목적입니다.
const throughput = new Trend('dispatch_throughput_per_second');

export const options = {
	scenarios: {
		// 한 번만 실행합니다. 부하를 주는 것이 아니라 한 발송의 소요시간을 재는 측정입니다.
		measure: {
			executor: 'shared-iterations',
			vus: 1,
			iterations: 1,
			maxDuration: `${MAX_WAIT_MINUTES + 5}m`,
		},
	},
	summaryTrendStats: ['avg'],
};

export default function () {
	const notificationId = createNotification();
	const queuedMillis = dispatch(notificationId);
	const status = waitForCompletion(notificationId);

	report(notificationId, queuedMillis, status);
}

/**
 * 수신자 수만 넘겨 발송 요청을 접수합니다. 접수 시간은 이 측정의 대상이 아닙니다.
 */
function createNotification() {
	const payload = JSON.stringify({
		title: '규모별 발송 소요시간 측정',
		content: '부하 테스트용 발송 요청입니다.',
		channel: 'SMS',
		scheduledAt: '2026-08-10T02:00:00',
		recipientCount: RECIPIENT_COUNT,
	});

	const response = http.post(`${BASE_URL}/api/test/notifications`, payload, {
		...JSON_HEADERS,
		timeout: '10m',
	});
	if (response.status !== 201) {
		throw new Error(`발송 요청 접수 실패: status=${response.status} body=${response.body}`);
	}
	return response.json('data.notificationId');
}

/**
 * 발송을 실행합니다. 응답이 곧바로 오며, 이 시점에는 아직 아무것도 발송되지 않았습니다.
 *
 * @returns 발송 지시를 쌓는 데 걸린 시간(ms)
 */
function dispatch(notificationId) {
	const response = http.post(`${BASE_URL}/api/notifications/${notificationId}/dispatch`, null, {
		...JSON_HEADERS,
		timeout: '30m',
	});
	if (response.status !== 202) {
		throw new Error(`발송 접수 실패: status=${response.status} body=${response.body}`);
	}

	const queuedMillis = response.json('data.elapsedMillis');
	queueDuration.add(queuedMillis);
	console.log(
		`[queue] notificationId=${notificationId} queued=${response.json('data.queuedCount')} ` +
		`elapsedMillis=${queuedMillis}`
	);
	return queuedMillis;
}

/**
 * 발송 요청이 완료 상태가 될 때까지 현황 조회를 반복합니다.
 *
 * <p>폴링 간격이 결과에 섞이지 않는 이유는, 소요시간을 이쪽에서 재지 않고 서버가 기록한
 * elapsedMillis를 읽기 때문입니다. 간격은 조회 부하와 확인 지연 사이의 절충일 뿐입니다.
 */
function waitForCompletion(notificationId) {
	const deadline = Date.now() + MAX_WAIT_MINUTES * 60 * 1000;

	while (Date.now() < deadline) {
		const response = http.get(`${BASE_URL}/api/notifications/${notificationId}`, {
			timeout: '60s',
		});
		if (response.status !== 200) {
			throw new Error(`발송 현황 조회 실패: status=${response.status} body=${response.body}`);
		}

		const status = response.json('data');
		if (status.status === 'COMPLETED') {
			return status;
		}

		console.log(
			`[wait] success=${status.successCount} fail=${status.failCount} pending=${status.pendingCount}`
		);
		sleep(POLL_INTERVAL_SECONDS);
	}

	throw new Error(
		`${MAX_WAIT_MINUTES}분 안에 발송이 끝나지 않았습니다. ` +
		`워커가 떠 있는지, DLQ로 빠진 건이 있는지 확인하십시오. notificationId=${notificationId}`
	);
}

function report(notificationId, queuedMillis, status) {
	dispatchDuration.add(status.elapsedMillis);

	const perSecond = status.elapsedMillis > 0
		? (status.totalCount / (status.elapsedMillis / 1000))
		: 0;
	throughput.add(perSecond);

	console.log(
		`[result] notificationId=${notificationId} total=${status.totalCount} ` +
		`success=${status.successCount} fail=${status.failCount} ` +
		`queueMillis=${queuedMillis} dispatchMillis=${status.elapsedMillis} ` +
		`perSecond=${perSecond.toFixed(2)}`
	);
}
