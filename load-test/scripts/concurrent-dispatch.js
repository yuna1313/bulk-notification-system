// 동시에 여러 발송을 실행하는 동안 다른 API가 응답할 수 있는지 측정합니다. (측정 C)
//
// v1은 발송 한 건이 수신자 수만큼 API 스레드와 DB 커넥션을 잡고 있습니다.
// 동시 발송 수가 커넥션 풀 크기(Hikari 기본 10)에 도달하면 접수 API까지 함께 막힙니다.
// "하나가 느린 것"과 "느린 하나가 전체를 막는 것"은 다른 문제이고, 후자가 v2가 해결할 문제입니다.
//
// 실행 예시
//   k6 run load-test/scripts/concurrent-dispatch.js
//   k6 run -e DISPATCH_VUS=10 -e RECIPIENT_COUNT=200 -e PROBE_DURATION=3m load-test/scripts/concurrent-dispatch.js
import http from 'k6/http';
import exec from 'k6/execution';
import { check } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const DISPATCH_VUS = Number(__ENV.DISPATCH_VUS || 10);
const RECIPIENT_COUNT = Number(__ENV.RECIPIENT_COUNT || 200);
const PROBE_DURATION = __ENV.PROBE_DURATION || '2m';

const JSON_HEADERS = { headers: { 'Content-Type': 'application/json' } };

// 발송이 도는 동안의 접수 API 응답시간입니다. 이 값이 이 테스트의 핵심 지표입니다.
const probeDuration = new Trend('probe_create_duration', true);

// 접수 API가 실제로 쓸 만한 상태였던 비율입니다. p(95)보다 읽기 쉬운 가용성 지표입니다.
const probeUnderOneSecond = new Rate('probe_create_under_1s');

export const options = {
	scenarios: {
		// 발송을 동시에 실행합니다. VU 하나가 발송 요청 하나를 맡습니다.
		dispatch: {
			executor: 'per-vu-iterations',
			vus: DISPATCH_VUS,
			iterations: 1,
			exec: 'dispatch',
			maxDuration: '60m',
		},
		// 발송이 도는 동안 접수 API를 초당 1회 호출해 응답 가능 여부를 확인합니다.
		//
		// 접수가 막히면 응답이 분 단위로 늘어납니다. VU가 모자라면 k6가 그 느린 반복을
		// 버려서(dropped_iterations) 가장 중요한 샘플이 통계에서 빠집니다.
		// 막힌 응답까지 전부 기록하도록 VU를 넉넉히 잡습니다.
		probe: {
			executor: 'constant-arrival-rate',
			rate: 1,
			timeUnit: '1s',
			duration: PROBE_DURATION,
			preAllocatedVUs: 50,
			maxVUs: 200,
			exec: 'probe',
		},
	},
	summaryTrendStats: ['avg', 'min', 'med', 'p(50)', 'p(95)', 'p(99)', 'max'],
};

/**
 * 발송할 요청을 미리 접수해 둡니다. 접수 시간은 측정 대상이 아니므로 여기서 처리합니다.
 */
export function setup() {
	const notificationIds = [];

	for (let index = 0; index < DISPATCH_VUS; index += 1) {
		const payload = JSON.stringify({
			title: `동시 발송 테스트 ${index + 1}`,
			content: '동시 발송 테스트용 발송 요청입니다.',
			channel: 'SMS',
			scheduledAt: '2026-08-10T02:00:00',
			recipientCount: RECIPIENT_COUNT,
		});

		const response = http.post(`${BASE_URL}/api/test/notifications`, payload, {
			...JSON_HEADERS,
			timeout: '600s',
		});

		if (response.status !== 201) {
			throw new Error(`발송 요청 접수 실패: status=${response.status} body=${response.body}`);
		}
		notificationIds.push(response.json('data.notificationId'));
	}

	console.log(`발송 요청 ${notificationIds.length}건 접수 완료 (수신자 각 ${RECIPIENT_COUNT}명)`);
	return { notificationIds };
}

/**
 * 미리 접수한 발송 요청을 실행합니다. 시나리오 안에서 반복 순번으로 요청을 나눠 가집니다.
 */
export function dispatch(data) {
	const index = exec.scenario.iterationInTest % data.notificationIds.length;
	const notificationId = data.notificationIds[index];

	const response = http.post(`${BASE_URL}/api/notifications/${notificationId}/dispatch`, null, {
		...JSON_HEADERS,
		timeout: '60m',
	});

	check(response, {
		'발송 실행 성공(200)': (r) => r.status === 200,
	});

	if (response.status === 200) {
		const body = response.json('data');
		console.log(
			`notificationId=${notificationId} total=${body.totalCount} success=${body.successCount} ` +
			`fail=${body.failCount} elapsedMillis=${body.elapsedMillis}`
		);
	}
}

/**
 * 발송이 도는 동안 접수 API가 응답하는지 확인합니다.
 */
export function probe() {
	const payload = JSON.stringify({
		title: '응답 확인',
		content: '발송 중 접수 API 응답을 확인합니다.',
		channel: 'SMS',
		scheduledAt: '2026-08-10T02:00:00',
		recipientCount: 1,
	});

	const response = http.post(`${BASE_URL}/api/test/notifications`, payload, {
		...JSON_HEADERS,
		timeout: '120s',
	});

	probeDuration.add(response.timings.duration);
	probeUnderOneSecond.add(response.timings.duration < 1000);

	// 상태 코드만 보면 80초 걸려 돌아온 응답도 성공으로 집계됩니다.
	// 실무에서 그것은 장애이므로 응답 시간 기준을 함께 확인합니다.
	check(response, {
		'접수 성공(201)': (r) => r.status === 201,
		'접수 1초 이내 응답': (r) => r.timings.duration < 1000,
	});
}
