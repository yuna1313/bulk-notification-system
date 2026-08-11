# load-test

발송 구조의 성능을 수치로 측정하는 k6 스크립트입니다.
v1(동기)과 v2(Kafka 기반 비동기)를 같은 조건에서 재고 비교하는 것이 목적입니다.

결과는 [`docs/load-test-v1.md`](../docs/load-test-v1.md), [`docs/load-test-v2.md`](../docs/load-test-v2.md)에 있습니다.

## 스크립트 세 개

| 파일 | 한 줄 요약 | 사용 측정 |
|---|---|---|
| `create-api.js` | **접수만** 잽니다. 발송은 아예 하지 않습니다 | D |
| `dispatch-duration.js` | **발송**을 잽니다. 10만 건 다 보내는 데 얼마나 걸리나 | A, B, E |
| `concurrent-dispatch.js` | **발송을 돌려놓고, 그동안 접수를 잽니다** | C |

측정 F(밀린 메시지 회복)는 스크립트가 없습니다. 아래 [F 절차](#f-밀린-메시지-회복-시간)를 따릅니다.

---

## 준비

### k6 설치

```bash
winget install k6 --source winget
```

```bash
choco install k6
```

설치 후 새 터미널에서 `k6 version`으로 확인합니다.
설치가 안 되면 <https://grafana.com/docs/k6/latest/set-up/install-k6/>에서 바이너리를 받습니다.

### Kafka 기동

```bash
docker compose up -d
```

`docker ps`에서 `notification-kafka`가 `Up ... (healthy)`가 될 때까지 기다립니다. 20~30초 걸립니다.

```bash
docker exec notification-kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list
```

목록이 나오면 정상입니다.

### 서버 기동

**반드시 `loadtest` 프로파일로 띄웁니다.** 기본 프로파일은 SQL 로그가 켜져 있어
접수·발행 경로가 60% 이상 느려집니다. API와 워커 **양쪽 모두** 적용해야 합니다.

```bash
# 1. 발송사 (포트 8081)
cd mock-provider && ./gradlew bootRun
```

```bash
# 2. 알림 서버 (포트 8080)
cd notification-api && ./gradlew build
java -Xmx2g -Dspring.profiles.active=loadtest -jar build/libs/api-0.0.1-SNAPSHOT.jar
```

```bash
# 3. 워커 (포트 8082)
cd notification-worker && ./gradlew build
java -Xmx2g -Dspring.profiles.active=loadtest -jar build/libs/worker-0.0.1-SNAPSHOT.jar
```

기동 로그에 아래 줄이 있는지 확인합니다. 없으면 프로파일이 안 걸린 것입니다.

```
The following 1 profile is active: "loadtest"
```

> **`-Dhttp.maxConnections`는 v2의 API에 필요 없습니다.** v1에서는 API가 직접 발송사를
> 호출했기 때문에 필요했습니다. v2에서 발송사를 호출하는 것은 워커이므로, 동시성을 크게
> 올려 측정할 때는 **워커 쪽에** `-Dhttp.maxConnections=300`을 붙입니다.
> 기본값 5로는 동시 발송이 수십 건을 넘어가면 매 호출이 새 TCP 소켓을 열고,
> Windows 임시 포트(16,384개)가 고갈되어 `BindException`이 발생합니다.
> 동시성 20 이하에서는 필요하지 않았습니다.

### 측정 전 초기화

이전 측정 데이터가 남아 있으면 조건이 달라집니다. **매 회차 전에 네 테이블을 모두 비웁니다.**

```sql
SET foreign_key_checks = 0;
TRUNCATE TABLE notification_message;
TRUNCATE TABLE notification;
TRUNCATE TABLE outbox_event;
TRUNCATE TABLE processed_event;
SET foreign_key_checks = 1;
```

`DELETE`가 아니라 `TRUNCATE`를 씁니다. `DELETE`는 테이블 파일 크기를 줄이지 않아
반복 측정 시 INSERT 성능이 점점 나빠집니다.

**컨슈머 그룹도 정리해야 합니다.** DB를 비워도 Kafka에 남은 메시지는 그대로여서,
워커가 DB에 없는 발송 건을 계속 읽고 건너뜁니다.

```bash
docker exec notification-kafka /opt/kafka/bin/kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 --group notification-worker \
  --reset-offsets --to-latest --topic notification.send --execute
```

**워커를 끈 상태에서 실행해야 합니다.** 활성 컨슈머가 있으면 거부됩니다.

**DLQ는 비우지 말고 회차 전후의 오프셋을 기록하십시오.** 토픽 삭제는 브로커 기동 실패를 유발한
적이 있어 권하지 않습니다. 아래 값을 회차 시작 전과 종료 후에 각각 재고 빼면 그 회차의
최종 실패 건수가 나옵니다. 기록하지 않으면 누적값만 남아 회차별 분리가 불가능합니다.

```bash
docker exec notification-kafka /opt/kafka/bin/kafka-get-offsets.sh \
  --bootstrap-server localhost:9092 --topic notification.send.dlq
```

파티션별로 출력되므로 전부 더한 값이 총 건수입니다.

### 발송사 조건 고정

측정 조건을 바꿀 때만 조정하고, 한 측정 안에서는 고정합니다.

```bash
curl -X PUT http://localhost:8081/config -H "Content-Type: application/json" \
  -d '{"latencyMs":200,"failureRate":0.03,"rateLimitPerSecond":1000}'
```

> `latencyMs`를 10,000 이상으로 올릴 때는 워커 `application.yml`의
> `notification.provider.read-timeout`도 함께 올려야 합니다.
> 그렇지 않으면 지연 재현이 전부 TIMEOUT 실패로 기록됩니다.

---

## 측정 항목

| 측정 | 내용 | 스크립트 |
|---|---|---|
| A | 수신자 규모별 발송 소요시간 | `dispatch-duration.js` |
| B | 발송사 지연 증가의 영향 | `dispatch-duration.js` |
| C | 동시 발송 중 접수 API 응답 | `concurrent-dispatch.js` |
| D | 접수 API 응답시간 | `create-api.js` |
| E | 파티션 수와 처리량 | `dispatch-duration.js` |
| F | 밀린 메시지 회복 시간 | 수동 절차 |

> **v1과 달라진 점** — v1은 발송 API가 완료 후 응답했으므로 A·B를 curl 한 번으로 잴 수 있었습니다.
> v2는 즉시 202를 반환하고 발송은 그 뒤에 일어나므로, 완료를 확인하려면 현황 조회를
> 반복해야 합니다. 그래서 A·B에도 스크립트를 씁니다.

### A. 수신자 규모별 발송 소요시간

```bash
k6 run -e RECIPIENT_COUNT=100 load-test/scripts/dispatch-duration.js
```

```bash
k6 run -e RECIPIENT_COUNT=10000 -e MAX_WAIT_MINUTES=60 load-test/scripts/dispatch-duration.js
```

```bash
k6 run -e RECIPIENT_COUNT=100000 -e MAX_WAIT_MINUTES=180 -e POLL_INTERVAL_SECONDS=10 load-test/scripts/dispatch-duration.js
```

기록할 값은 `[result]` 로그의 `dispatchMillis`와 `perSecond`입니다.

| 값 | 뜻 |
|---|---|
| `queueMillis` | 발송 실행 요청부터 202 응답까지. 지시를 쌓는 시간 |
| `dispatchMillis` | **발송 시작부터 마지막 발송까지. v1의 소요시간과 정의가 같습니다** |
| `perSecond` | 초당 발송 건수 |

> 지연 200ms × 100,000건은 파티션 10 기준 약 55분입니다.
> 100 → 10,000 순으로 절차를 확인한 뒤 마지막에 100,000건을 돌리십시오.

### B. 발송사 지연 증가의 영향

수신자 300명 고정, `latencyMs`만 200 → 1,000 → 3,000으로 바꿔가며 A를 반복합니다.

```bash
curl -X PUT http://localhost:8081/config -H "Content-Type: application/json" \
  -d '{"latencyMs":1000,"failureRate":0.03,"rateLimitPerSecond":1000}'
```

```bash
k6 run -e RECIPIENT_COUNT=300 load-test/scripts/dispatch-duration.js
```

### C. 동시 발송 중 접수 API 응답

```bash
k6 run -e DISPATCH_VUS=200 -e RECIPIENT_COUNT=2000 -e PROBE_DURATION=2m load-test/scripts/concurrent-dispatch.js
```

**핵심 지표는 `probe_create_duration`입니다.** 발송이 도는 동안 접수 API가 얼마나 느려지는지를
나타냅니다. k6 요약의 `http_req_duration`은 접수·발송실행·현황조회가 섞인 값이므로 쓰지 않습니다.

**`dispatch_wall_duration`이 `PROBE_DURATION`보다 길어야 측정이 유효합니다.**
프로브가 도는 내내 발송이 진행 중이었다는 뜻이기 때문입니다.
짧게 나오면 `RECIPIENT_COUNT`를 키워 다시 측정합니다.

> **v1과 같은 `DISPATCH_VUS`로 측정해야 비교가 성립합니다.** v1은 동시 발송 200건에서
> Tomcat 스레드(200개)가 고갈되는 지점을 찾았습니다. v2에서 이 병목이 사라졌는지 확인하려면
> 같은 조건으로 재야 합니다.

### D. 접수 API 응답시간

발송을 실행하지 않은 상태에서 접수만 측정합니다.

```bash
k6 run -e RECIPIENT_COUNT=1000 -e VUS=20 -e DURATION=1m load-test/scripts/create-api.js
```

`http_req_duration`의 p(50), p(95), p(99)와 `http_reqs`를 기록합니다.
행/초는 `http_reqs × RECIPIENT_COUNT`로 계산합니다.

**워커를 끈 상태에서 측정합니다.** 워커가 돌면 같은 DB에 UPDATE를 쏟아부어 조건이 달라집니다.

### E. 파티션 수와 처리량

수신자 10,000명 고정, 파티션 수와 워커 동시성을 같은 값으로 맞춰가며 측정합니다.

**파티션은 늘릴 수만 있고 줄일 수 없습니다.** 반드시 **오름차순(5 → 10 → 20)**으로 진행하십시오.

**1) 파티션 5로 시작** — 토픽이 없는 상태에서 `notification-api`의
`notification.kafka.send-topic.partitions`를 5로 두고 기동하면 5개로 생성됩니다.

**2) 다음 회차부터는 `--alter`로 늘립니다**

```bash
docker exec notification-kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 --alter --topic notification.send --partitions 10
```

**3) 매 회차 공통 절차**

1. 워커 종료
2. 위 `--alter` 실행
3. `notification-worker`의 `spring.kafka.listener.concurrency`를 같은 값으로 수정
4. `notification-api`의 `notification.kafka.send-topic.partitions`도 같은 값으로 수정 (기록용)
5. DB 초기화 + 컨슈머 오프셋 리셋
6. API 재기동 → 워커 기동
7. 측정

```bash
k6 run -e RECIPIENT_COUNT=10000 -e MAX_WAIT_MINUTES=60 load-test/scripts/dispatch-duration.js
```

파티션 수는 실행 전에 확인합니다.

```bash
docker exec notification-kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 --describe --topic notification.send
```

> **토픽을 삭제하지 마십시오.** 삭제 후 브로커가 남은 디렉터리를 정리하는 과정에서
> 기동 실패가 발생한 적이 있습니다. 오름차순으로 진행하면 삭제할 일이 없습니다.

### F. 밀린 메시지 회복 시간

워커를 끈 채로 대량 발송을 쌓아두고, 워커를 켠 뒤 lag이 0이 될 때까지의 시간을 잽니다.

**1) DB 초기화, 워커 켜기**

**2) 소량 발송으로 컨슈머 그룹 등록** — 그룹이 없으면 lag을 조회할 수 없습니다.

```bash
k6 run -e RECIPIENT_COUNT=100 load-test/scripts/dispatch-duration.js
```

**3) 워커 종료**

**4) 10만 건 발송 실행** — 완료를 기다리면 안 되므로 스크립트가 아니라 API를 직접 호출합니다.

```powershell
$response = Invoke-RestMethod -Method POST -Uri "http://localhost:8080/api/test/notifications" -ContentType "application/json" -Body '{"title":"lag test","content":"hello","channel":"SMS","scheduledAt":"2026-08-10T02:00:00","recipientCount":100000}'
$ID = $response.data.notificationId
Invoke-RestMethod -Method POST -Uri "http://localhost:8080/api/notifications/$ID/dispatch"
```

**5) outbox 발행 완료 대기**

```sql
SELECT COUNT(*) FROM outbox_event WHERE status = 'PENDING';
```

0이 될 때까지 기다립니다. 10만 건이면 3~4분 걸립니다.

**6) lag 확인** — 10만 근처가 나와야 합니다.

```bash
docker exec notification-kafka /opt/kafka/bin/kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 --describe --group notification-worker
```

**7) 관측 시작 후 곧바로 워커 기동**

PowerShell:

```powershell
$started = $null
while ($true) {
  $now = Get-Date
  $out = docker exec notification-kafka /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group notification-worker
  $nums = @()
  foreach ($line in $out) {
    $f = ($line.Trim() -split '\s+')
    if ($f.Count -ge 6 -and $f[5] -match '^\d+$') { $nums += [long]$f[5] }
  }
  if ($nums.Count -gt 0) {
    $lag = ($nums | Measure-Object -Sum).Sum
    Write-Host "$($now.ToString('HH:mm:ss')) LAG=$lag"
    if (-not $started -and $lag -gt 0) { $started = $now; Write-Host ">>> 타이머 시작" }
    if ($started -and $lag -eq 0) {
      Write-Host ">>> 회복 완료: $([int]($now - $started).TotalSeconds)초"
      break
    }
  } else {
    Write-Host "$($now.ToString('HH:mm:ss')) LAG=NA"
  }
  Start-Sleep -Seconds 5
}
```

Git Bash:

```bash
started=""; while true; do now=$(date +%s); lag=$(docker exec notification-kafka /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group notification-worker 2>/dev/null | awk 'NR>1 && $6 ~ /^[0-9]+$/ {s+=$6; n++} END {if (n>0) print s; else print "NA"}'); echo "$(date +%H:%M:%S) LAG=$lag"; if [ -z "$started" ] && [ "$lag" != "NA" ] && [ "$lag" -gt 0 ] 2>/dev/null; then started=$now; echo ">>> 타이머 시작"; fi; if [ -n "$started" ] && [ "$lag" = "0" ]; then echo ">>> 회복 완료: $((now-started))초"; break; fi; sleep 5; done
```

**관측 시작과 워커 기동 사이를 짧게 유지하십시오.** 그 사이 시간이 결과에 그대로 들어갑니다.

`LAG=NA`는 컨슈머 그룹이 없다는 뜻입니다. 2번 단계를 건너뛴 것이니 다시 하십시오.

---

## 결과 확인용 쿼리

**상태별 집계**

```sql
SELECT status, COUNT(*) FROM notification_message GROUP BY status;
```

**outbox 정합성** — 토픽 발행 수와 DB 행 수가 같아야 합니다.

```bash
docker exec notification-kafka /opt/kafka/bin/kafka-get-offsets.sh \
  --bootstrap-server localhost:9092 --topic notification.send
```

**DLQ 확인**

```bash
docker exec notification-kafka /opt/kafka/bin/kafka-get-offsets.sh \
  --bootstrap-server localhost:9092 --topic notification.send.dlq
```

**커넥션 풀 병목 확인** — 부하가 도는 중에 실행합니다.

```bash
curl http://localhost:8080/actuator/metrics/hikaricp.connections.active
```

```bash
curl http://localhost:8080/actuator/metrics/hikaricp.connections.pending
```

`active`가 최대치(10)에 고정되고 `pending`이 쌓이면 병목은 커넥션 풀입니다.
워커는 포트 8082로 같은 경로를 확인합니다.

---

## 결과 기록

- v1 → [`docs/load-test-v1.md`](../docs/load-test-v1.md)
- v2 → [`docs/load-test-v2.md`](../docs/load-test-v2.md)

k6 원본 출력을 남기려면 `--out json=results/이름.json`을 붙입니다. `results/`는 커밋되지 않습니다.
