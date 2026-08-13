# load-test

v1과 v2의 발송 구조를 측정하기 위한 k6 부하 테스트입니다.

이 문서는 **테스트 실행 환경과 재현 방법만 정리합니다.**

측정 결과와 분석은 아래 문서에 있습니다.

- [v1 부하 테스트 결과](../docs/load-test-v1.md)
- [v2 부하 테스트 결과](../docs/load-test-v2.md)

---

## 테스트 환경

- Java 17
- MySQL 8
- Kafka
- k6
- `mock-provider`
- `notification-api`
- `notification-worker`

v2 테스트에서는 Kafka 파티션 10, Worker Consumer 동시성 10을 기본값으로 사용합니다.

---

## 1. 실행 준비

로컬에 MySQL 8과 Kafka가 필요합니다.

개발 및 테스트 과정에서는 IntelliJ IDEA에서 각 Spring Boot 애플리케이션을 실행했습니다.  
부하 테스트 시 `notification-api`와 `notification-worker`는 `loadtest` 프로파일을 적용해 실행했습니다.

아래 명령어는 IDE를 사용하지 않고 동일한 환경으로 실행할 경우 참고할 수 있습니다.

### Kafka

```bash
docker compose up -d
```

정상 실행 여부를 확인합니다.

```bash
docker ps
```

### mock-provider

```bash
cd mock-provider
./gradlew bootRun
```

기본 포트는 `8081`입니다.

### notification-api

부하 테스트에서는 SQL 로그 등의 영향을 줄이기 위해 `loadtest` 프로파일로 실행합니다.

```bash
cd notification-api

./gradlew build

java -Xmx2g \
  -Dspring.profiles.active=loadtest \
  -jar build/libs/api-0.0.1-SNAPSHOT.jar
```

기본 포트는 `8080`입니다.

### notification-worker

```bash
cd notification-worker

./gradlew build

java -Xmx2g \
  -Dspring.profiles.active=loadtest \
  -jar build/libs/worker-0.0.1-SNAPSHOT.jar
```

기본 포트는 `8082`입니다.

---

## 2. 테스트 전 초기화

각 측정 결과가 이전 테스트의 영향을 받지 않도록 DB를 초기화합니다.

```sql
SET foreign_key_checks = 0;

TRUNCATE TABLE notification_message;
TRUNCATE TABLE notification;
TRUNCATE TABLE outbox_event;
TRUNCATE TABLE processed_event;

SET foreign_key_checks = 1;
```

Kafka에 이전 테스트의 메시지가 남아 있다면 Consumer Offset도 정리합니다.

Worker를 종료한 상태에서 실행합니다.

```bash
docker exec notification-kafka \
  /opt/kafka/bin/kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --group notification-worker \
  --reset-offsets \
  --to-latest \
  --topic notification.send \
  --execute
```

---

## 3. mock-provider 조건

기본 테스트 조건은 다음과 같습니다.

```bash
curl -X PUT http://localhost:8081/config \
  -H "Content-Type: application/json" \
  -d '{"latencyMs":200,"failureRate":0.03,"rateLimitPerSecond":1000}'
```

측정 B에서는 `latencyMs`만 변경합니다.

---

## 4. 측정 항목

| 측정 | 내용                                         |
| ---- | -------------------------------------------- |
| A    | 수신자 규모별 발송 소요시간                  |
| B    | 외부 발송사 지연 증가의 영향                 |
| C    | 동시 발송 중 접수 API 응답시간               |
| D    | 접수 API 자체 성능                           |
| E    | Kafka 파티션 / Consumer 동시성에 따른 처리량 |
| F    | Worker 중단 후 Consumer lag 회복             |

---

## A. 수신자 규모별 발송시간

```bash
# 100명
k6 run -e RECIPIENT_COUNT=100 \
  load-test/scripts/dispatch-duration.js

# 1,000명
k6 run -e RECIPIENT_COUNT=1000 \
  load-test/scripts/dispatch-duration.js

# 10,000명
k6 run -e RECIPIENT_COUNT=10000 \
  -e MAX_WAIT_MINUTES=60 \
  load-test/scripts/dispatch-duration.js

# 100,000명
k6 run -e RECIPIENT_COUNT=100000 \
  -e MAX_WAIT_MINUTES=180 \
  -e POLL_INTERVAL_SECONDS=10 \
  load-test/scripts/dispatch-duration.js
```

주요 확인 값:

- `queueMillis`: 발송 요청부터 Outbox 적재 후 `202` 응답까지 걸린 시간
- `dispatchMillis`: 실제 발송 시작부터 마지막 발송까지 걸린 시간
- `perSecond`: 초당 발송 처리량

---

## B. 외부 발송사 지연

수신자 수는 300명으로 고정하고 `mock-provider`의 `latencyMs`를 변경합니다.

```text
200ms → 1,000ms → 3,000ms
```

각 조건에서 실행합니다.

```bash
k6 run -e RECIPIENT_COUNT=300 \
  load-test/scripts/dispatch-duration.js
```

---

## C. 동시 발송 중 접수 API 응답

수신자 수를 200명으로 고정하고 동시 발송 요청 수를 변경합니다.

```bash
k6 run -e DISPATCH_VUS=50 \
  -e RECIPIENT_COUNT=200 \
  -e PROBE_DURATION=2m \
  load-test/scripts/concurrent-dispatch.js
```

`DISPATCH_VUS`를 다음 순서로 변경해 측정합니다.

```text
50 → 100 → 200 → 250
```

주요 확인 값은 `probe_create_duration`의 p(50), p(95), p(99)입니다.

`dispatch_wall_duration`이 `PROBE_DURATION`보다 길어야 발송이 진행되는 동안 접수 API를 측정한 결과로 볼 수 있습니다.

---

## D. 접수 API 응답시간

발송을 실행하지 않고 접수 API만 측정합니다.

**Worker를 종료한 상태에서 실행합니다.**

```bash
# 수신자 100명 / VU 10
k6 run -e RECIPIENT_COUNT=100 \
  -e VUS=10 \
  -e DURATION=1m \
  load-test/scripts/create-api.js

# 수신자 1,000명 / VU 10
k6 run -e RECIPIENT_COUNT=1000 \
  -e VUS=10 \
  -e DURATION=1m \
  load-test/scripts/create-api.js

# 수신자 1,000명 / VU 20
k6 run -e RECIPIENT_COUNT=1000 \
  -e VUS=20 \
  -e DURATION=1m \
  load-test/scripts/create-api.js
```

주요 확인 값:

- `http_req_duration` p(50)
- p(95)
- p(99)
- `http_reqs`
- `http_req_failed`

---

## E. 파티션 / Consumer 동시성

수신자 수는 10,000명으로 고정합니다.

Kafka 파티션 수와 Worker Consumer 동시성을 동일하게 맞춰 측정합니다.

```text
5 / 5
→ 10 / 10
→ 20 / 20
```

파티션은 실행 중 줄일 수 없으므로 작은 값부터 순서대로 증가시킵니다.

```bash
docker exec notification-kafka \
  /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --alter \
  --topic notification.send \
  --partitions 10
```

Worker의 `spring.kafka.listener.concurrency`도 동일한 값으로 변경한 뒤 실행합니다.

```bash
k6 run -e RECIPIENT_COUNT=10000 \
  -e MAX_WAIT_MINUTES=60 \
  load-test/scripts/dispatch-duration.js
```

---

## F. Worker 중단 후 lag 회복

Worker를 종료한 상태에서 대량의 메시지를 Kafka에 쌓습니다.

이후 Worker를 다시 실행하고 Consumer lag이 `0`이 될 때까지 확인합니다.

Consumer lag은 다음 명령으로 확인할 수 있습니다.

```bash
docker exec notification-kafka \
  /opt/kafka/bin/kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --describe \
  --group notification-worker
```

`LAG`은 Kafka에 들어왔지만 아직 Consumer가 처리하지 못한 메시지 수를 의미합니다.

---

## 결과 확인

DB 상태별 발송 결과:

```sql
SELECT status, COUNT(*)
FROM notification_message
GROUP BY status;
```

DLQ 메시지 수:

```bash
docker exec notification-kafka \
  /opt/kafka/bin/kafka-get-offsets.sh \
  --bootstrap-server localhost:9092 \
  --topic notification.send.dlq
```

부하 중 DB Connection Pool 상태를 확인하려면 다음 메트릭을 사용합니다.

```bash
curl http://localhost:8080/actuator/metrics/hikaricp.connections.active
curl http://localhost:8080/actuator/metrics/hikaricp.connections.pending
```

Worker는 포트 `8082`에서 같은 메트릭을 확인할 수 있습니다.
