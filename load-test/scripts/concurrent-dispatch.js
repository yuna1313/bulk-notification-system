// 동시에 여러 발송을 실행하는 동안 다른 API가 응답할 수 있는지 측정합니다. (측정 C)
//
// v1은 발송 한 건이 수신자 수만큼 API 스레드와 DB 커넥션을 잡고 있습니다.
// 동시 발송 수가 커넥션 풀 크기(Hikari 기본 10)에 도달하면 접수 API까지 함께 막힙니다.
// "하나가 느린 것"과 "느린 하나가 전체를 막는 것"은 다른 문제이고, 후자가 v2가 해결할 문제입니다.
//
// v2는 POST /dispatch가 발송 지시만 쌓고 1초 안에 응답합니다. 실제 발송은 그 뒤에 워커에서
// 일어나므로, 요청이 끝난 시점과 발송이 끝난 시점이 크게 벌어집니다. 프로브는 그와 무관하게
// PROBE_DURATION 동안 돌기 때문에, 발송이 먼저 끝나면 남은 구간은 아무 부하도 없는 API를
// 재게 되고 결과가 실제보다 좋게 나옵니다.
//
// 그래서 발송을 맡은 VU가 완료될 때까지 현황 조회로 기다리게 했습니다. 프로브 구간을 늘리려는
// 것이 아니라, 발송이 실제로 몇 초 걸렸는지를 dispatch_wall_duration으로 남기기 위해서입니다.
// 그 값이 없으면 프로브가 잰 구간 중 어디까지가 유효한지 판단할 수 없습니다.
//
// 측정을 유효하게 만드는 것은 파라미터입니다. 발송 소요 시간이 PROBE_DURATION보다 길도록
// RECIPIENT_COUNT를 잡고, 실행 후 dispatch_wall_duration으로 그렇게 됐는지 확인하십시오.
//
// 실행 예시
//   k6 run load-test/scripts/concurrent-dispatch.js
//   k6 run -e DISPATCH_VUS=10 -e RECIPIENT_COUNT=2000 -e PROBE_DURATION=2m load-test/scripts/concurrent-dispatch.js
import http from "k6/http";
import exec from "k6/execution";
import { check, sleep } from "k6";
import { Rate, Trend } from "k6/metrics";

const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";
const DISPATCH_VUS = Number(__ENV.DISPATCH_VUS || 10);
const RECIPIENT_COUNT = Number(__ENV.RECIPIENT_COUNT || 200);
const PROBE_DURATION = __ENV.PROBE_DURATION || "2m";
const POLL_INTERVAL_SECONDS = Number(__ENV.POLL_INTERVAL_SECONDS || 10);
const MAX_WAIT_MINUTES = Number(__ENV.MAX_WAIT_MINUTES || 30);

const JSON_HEADERS = { headers: { "Content-Type": "application/json" } };

// 발송이 도는 동안의 접수 API 응답시간입니다. 이 값이 이 테스트의 핵심 지표입니다.
const probeDuration = new Trend("probe_create_duration", true);

// 접수 API가 실제로 쓸 만한 상태였던 비율입니다. p(95)보다 읽기 쉬운 가용성 지표입니다.
const probeUnderOneSecond = new Rate("probe_create_under_1s");

// 발송 실행부터 완료까지의 실제 시간입니다. 프로브 구간이 발송 구간 안에 들어 있었는지를
// 판단하는 근거이며, PROBE_DURATION보다 커야 측정이 유효합니다.
const dispatchWallDuration = new Trend("dispatch_wall_duration", true);

export const options = {
  scenarios: {
    // 발송을 동시에 실행합니다. VU 하나가 발송 요청 하나를 맡습니다.
    dispatch: {
      executor: "per-vu-iterations",
      vus: DISPATCH_VUS,
      iterations: 1,
      exec: "dispatch",
      maxDuration: "60m",
    },
    // 발송이 도는 동안 접수 API를 초당 1회 호출해 응답 가능 여부를 확인합니다.
    //
    // 접수가 막히면 응답이 분 단위로 늘어납니다. VU가 모자라면 k6가 그 느린 반복을
    // 버려서(dropped_iterations) 가장 중요한 샘플이 통계에서 빠집니다.
    // 막힌 응답까지 전부 기록하도록 VU를 넉넉히 잡습니다.
    probe: {
      executor: "constant-arrival-rate",
      rate: 1,
      timeUnit: "1s",
      duration: PROBE_DURATION,
      preAllocatedVUs: 50,
      maxVUs: 200,
      exec: "probe",
    },
  },
  summaryTrendStats: ["avg", "min", "med", "p(50)", "p(95)", "p(99)", "max"],
};

/**
 * 발송할 요청을 미리 접수해 둡니다. 접수 시간은 측정 대상이 아니므로 여기서 처리합니다.
 */
export function setup() {
  const notificationIds = [];

  for (let index = 0; index < DISPATCH_VUS; index += 1) {
    const payload = JSON.stringify({
      title: `동시 발송 테스트 ${index + 1}`,
      content: "동시 발송 테스트용 발송 요청입니다.",
      channel: "SMS",
      scheduledAt: "2026-08-10T02:00:00",
      recipientCount: RECIPIENT_COUNT,
    });

    const response = http.post(`${BASE_URL}/api/test/notifications`, payload, {
      ...JSON_HEADERS,
      timeout: "600s",
    });

    if (response.status !== 201) {
      throw new Error(
        `발송 요청 접수 실패: status=${response.status} body=${response.body}`,
      );
    }
    notificationIds.push(response.json("data.notificationId"));
  }

  console.log(
    `발송 요청 ${notificationIds.length}건 접수 완료 (수신자 각 ${RECIPIENT_COUNT}명)`,
  );
  return { notificationIds };
}

/**
 * 미리 접수한 발송 요청을 실행하고, 그 발송이 끝날 때까지 붙잡고 있습니다.
 *
 * <p>기다리는 이유는 발송이 실제로 언제 끝났는지 알기 위해서입니다. 요청을 던지고 바로 끝내면
 * 202를 받은 시점만 알 수 있고, 그 뒤 워커에서 얼마나 더 걸렸는지는 알 수 없습니다.
 */
export function dispatch(data) {
  const index = exec.scenario.iterationInTest % data.notificationIds.length;
  const notificationId = data.notificationIds[index];

  const startedAt = Date.now();
  const response = http.post(
    `${BASE_URL}/api/notifications/${notificationId}/dispatch`,
    null,
    {
      ...JSON_HEADERS,
      timeout: "10m",
    },
  );

  check(response, {
    "발송 접수 성공(202)": (r) => r.status === 202,
  });
  if (response.status !== 202) {
    return;
  }

  const body = response.json("data");
  console.log(
    `notificationId=${notificationId} queued=${body.queuedCount} queueMillis=${body.elapsedMillis}`,
  );

  waitForCompletion(notificationId);

  const wallMillis = Date.now() - startedAt;
  dispatchWallDuration.add(wallMillis);
  console.log(
    `notificationId=${notificationId} 발송 완료까지 ${(wallMillis / 1000).toFixed(1)}초`,
  );
}

/**
 * 발송 요청이 완료 상태가 될 때까지 현황 조회를 반복합니다.
 *
 * <p>조회 간격을 짧게 잡지 마십시오. 현황 조회는 매번 상태별로 집계하는 쿼리라 그 자체가
 * API에 부하를 줍니다. 프로브가 재는 것이 그 API이므로, 조회가 잦으면 측정 대상이
 * 측정 행위 때문에 느려집니다.
 */
function waitForCompletion(notificationId) {
  const deadline = Date.now() + MAX_WAIT_MINUTES * 60 * 1000;

  while (Date.now() < deadline) {
    const response = http.get(
      `${BASE_URL}/api/notifications/${notificationId}`,
      {
        timeout: "120s",
      },
    );
    if (
      response.status === 200 &&
      response.json("data.status") === "COMPLETED"
    ) {
      return;
    }
    sleep(POLL_INTERVAL_SECONDS);
  }

  console.warn(
    `발송이 ${MAX_WAIT_MINUTES}분 안에 끝나지 않았습니다. ` +
      `워커가 떠 있는지, DLQ로 빠진 건이 있는지 확인하십시오. notificationId=${notificationId}`,
  );
}

/**
 * 발송이 도는 동안 접수 API가 응답하는지 확인합니다.
 */
export function probe() {
  const payload = JSON.stringify({
    title: "응답 확인",
    content: "발송 중 접수 API 응답을 확인합니다.",
    channel: "SMS",
    scheduledAt: "2026-08-10T02:00:00",
    recipientCount: 1,
  });

  const response = http.post(`${BASE_URL}/api/test/notifications`, payload, {
    ...JSON_HEADERS,
    timeout: "120s",
  });

  probeDuration.add(response.timings.duration);
  probeUnderOneSecond.add(response.timings.duration < 1000);

  // 상태 코드만 보면 80초 걸려 돌아온 응답도 성공으로 집계됩니다.
  // 실무에서 그것은 장애이므로 응답 시간 기준을 함께 확인합니다.
  check(response, {
    "접수 성공(201)": (r) => r.status === 201,
    "접수 1초 이내 응답": (r) => r.timings.duration < 1000,
  });
}
