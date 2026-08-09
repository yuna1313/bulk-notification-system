# load-test

v1(동기 처리) 구조의 한계를 수치로 측정하는 k6 스크립트입니다.

## 준비

### k6 설치

```bash
winget install k6 --source winget
# 또는
choco install k6
```

설치 후 새 터미널에서 `k6 version`으로 확인합니다.
설치가 안 되면 <https://grafana.com/docs/k6/latest/set-up/install-k6/>에서 바이너리를 받습니다.

### 서버 기동

부하 테스트는 반드시 `loadtest` 프로파일로 띄웁니다. 기본 프로파일은 SQL 로그가 켜져 있어
10만 건 발송 시 로그만 10만 줄이 쌓이고, 그 디스크 I/O가 측정 대상인 발송 시간에 섞입니다.

```bash
# 발송사 (포트 8081)
cd mock-provider && ./gradlew bootRun

# 알림 서버 (포트 8080) — 힙을 넉넉히 줍니다
cd notification-api && ./gradlew build
java -Xmx2g -Dhttp.maxConnections=300 -jar build/libs/api-0.0.1-SNAPSHOT.jar
```

`-Dhttp.maxConnections`는 발송사로 나가는 HTTP 커넥션의 재사용 캐시 크기입니다. 기본값이 5라
동시 발송이 수십 건을 넘어가면 매 호출이 새 TCP 소켓을 열고 닫습니다. 닫힌 소켓은 TIME_WAIT로
약 2분간 남고 Windows 임시 포트는 16,384개뿐이어서, 동시 발송 200건 구간에서 포트가 고갈되며
`BindException: Address already in use`가 발생합니다.

이 값은 동시 발송 수보다 크게 잡습니다. 발송사가 느려서 실패한 것과 소켓 자원이 없어서 실패한
것을 섞지 않으려면 필요한 설정입니다.

### 측정 전 초기화

이전 측정 데이터가 남아 있으면 상태 집계 쿼리가 느려져 조건이 달라집니다. 매 측정 전에 비웁니다.

```sql
TRUNCATE TABLE notification_message;
DELETE FROM notification;
ALTER TABLE notification AUTO_INCREMENT = 1;
```

### 발송사 조건 고정

측정 조건을 바꿀 때만 이 값을 조정하고, 한 측정 안에서는 고정합니다.

```bash
curl -X PUT http://localhost:8081/config -H "Content-Type: application/json" \
  -d '{"latencyMs":200,"failureRate":0.03,"rateLimitPerSecond":1000}'
```

> `latencyMs`를 10,000 이상으로 올릴 때는 `application.yml`의 `notification.provider.read-timeout`도
> 함께 올려야 합니다. 그렇지 않으면 지연 재현이 전부 TIMEOUT 실패로 기록됩니다.

## 측정 항목

| 측정 | 내용 | 방법 |
|---|---|---|
| A | 수신자 규모별 발송 소요시간 | curl (k6 불필요) |
| B | 발송사 지연 증가의 영향 | curl (k6 불필요) |
| C | 동시 발송 시 커넥션 풀·스레드 점유 | `concurrent-dispatch.js` |
| D | 접수 API 응답시간 p50·p95·p99 | `create-api.js` |

A와 B는 발송을 한 번 실행하고 응답의 `elapsedMillis`를 읽으면 되므로 부하 도구가 필요 없습니다.

### A. 수신자 규모별 발송 소요시간

100 / 10,000 / 100,000건으로 반복하고 `elapsedMillis`를 기록합니다.

```bash
curl -X POST http://localhost:8080/api/test/notifications \
  -H "Content-Type: application/json" \
  -d '{"title":"규모별 측정","content":"test","channel":"SMS","scheduledAt":"2026-08-10T02:00:00","recipientCount":100}'

curl -X POST http://localhost:8080/api/notifications/1/dispatch
```

> 지연 200ms × 100,000건은 산술적으로 약 5.5시간입니다. 100 → 1,000 → 10,000 순으로 올리며
> 절차를 확인한 뒤 마지막에 100,000건을 돌리십시오.

### B. 발송사 지연 증가의 영향

`latencyMs`를 200 → 1,000 → 3,000으로 바꿔가며 A를 반복합니다.

### C. 동시 발송

```bash
k6 run load-test/scripts/concurrent-dispatch.js
k6 run -e DISPATCH_VUS=10 -e RECIPIENT_COUNT=200 -e PROBE_DURATION=3m load-test/scripts/concurrent-dispatch.js
```

`probe_create_duration`이 이 측정의 핵심 지표입니다. 발송이 도는 동안 접수 API가 얼마나 느려지는지를
나타냅니다. 동시 발송 수를 1 → 5 → 10 → 20으로 올리며 접수 API가 막히기 시작하는 지점을 찾습니다.

Hikari 기본 풀 크기가 10이므로 동시 발송 10건 부근에서 접수 API가 응답하지 못하기 시작할 것으로
예상합니다. 이 예상이 맞는지 확인하는 것이 목적입니다.

### D. 접수 API 응답시간

```bash
k6 run load-test/scripts/create-api.js
k6 run -e RECIPIENT_COUNT=1000 -e VUS=20 -e DURATION=1m load-test/scripts/create-api.js
```

`http_req_duration`의 p(50), p(95), p(99)를 기록합니다.

## 결과 기록

측정 결과는 [`docs/load-test-v1.md`](../docs/load-test-v1.md)에 기록합니다.
k6 원본 출력을 남기려면 `--out json=results/이름.json`을 붙입니다. `results/`는 커밋되지 않습니다.
