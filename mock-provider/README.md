# mock-provider

외부 알림 발송사(SMS · 푸시 업체)를 흉내 내는 테스트용 서버입니다.

**실제 발송은 하지 않으며, 요청을 받아 설정된 시간만큼 기다렸다가 성공 또는 실패를 응답합니다.

## 왜 필요한가

`notification-api`의 부하 특성을 측정하려면 외부 발송사의 **지연과 실패를 원하는 시점에 원하는 크기로 만들 수 있어야** 합니다. 실제 발송사 API로는 이것이 불가능합니다.

- 테스트로 수만 건을 발송할 수 없습니다. (비용)
- "지금부터 응답을 3초로 늦춰 달라"고 요청할 수 없습니다.

또한 발송 로직을 같은 애플리케이션 안의 함수로 두고 `Thread.sleep()`을 거는 방식으로는 실제 HTTP 커넥션 점유나 커넥션 풀 고갈이 재현되지 않습니다. 그래서 별도 서버로 분리했습니다.

`PUT /config`로 지연시간과 실패율을 **실행 중에** 바꿀 수 있으며, 이것이 이 모듈의 핵심 기능입니다.

## 실행

```bash
./gradlew bootRun
```

기본 포트는 `8081`입니다. DB를 사용하지 않으므로 별도 준비물은 없습니다.

## API

### POST /send

발송 요청을 받습니다. 설정된 지연시간만큼 대기한 뒤 응답합니다.

**Request**

```json
{
  "messageId": "msg-0001",
  "recipientId": "user-123",
  "channel": "SMS",
  "content": "예약 안내드립니다."
}
```

**Response — 200 OK**

```json
{
  "messageId": "msg-0001",
  "status": "SUCCESS",
  "providerMessageId": "prv-8a3f21c0"
}
```

**Response — 500 Internal Server Error**

설정된 실패율에 따라 확률적으로 발생합니다.

```json
{
  "messageId": "msg-0001",
  "status": "FAILED",
  "reason": "PROVIDER_ERROR"
}
```

**Response — 429 Too Many Requests**

초당 요청 수가 한도를 넘으면 **지연 없이 즉시** 반환됩니다.

```json
{
  "messageId": "msg-0001",
  "status": "REJECTED",
  "reason": "RATE_LIMIT_EXCEEDED"
}
```

### PUT /config

동작 설정을 변경합니다. **서버 재시작 없이 즉시 반영**되며, 일부 필드만 보내면 나머지는 기존값이 유지됩니다.

**Request**

```json
{
  "latencyMs": 3000,
  "failureRate": 0.1,
  "rateLimitPerSecond": 100
}
```

| 필드 | 설명 | 기본값 |
|---|---|---|
| `latencyMs` | 응답까지의 지연시간 (ms) | `200` |
| `failureRate` | 실패 확률 (0.0 ~ 1.0) | `0.03` |
| `rateLimitPerSecond` | 초당 허용 요청 수 | `1000` |

### GET /config

현재 설정값을 조회합니다.

## 부하 테스트용 커맨드

부하 테스트 중 발송사 상태를 바꿀 때 사용합니다.

```bash
# 현재 설정 확인
curl http://localhost:8081/config

# 정상 상태 (기본값)
curl -X PUT http://localhost:8081/config \
  -H "Content-Type: application/json" \
  -d '{"latencyMs":200,"failureRate":0.03,"rateLimitPerSecond":1000}'

# 발송사 지연 상황 — 응답이 3초로 느려짐
curl -X PUT http://localhost:8081/config \
  -H "Content-Type: application/json" \
  -d '{"latencyMs":3000}'

# 발송사 장애 상황 — 전건 실패
curl -X PUT http://localhost:8081/config \
  -H "Content-Type: application/json" \
  -d '{"failureRate":1.0}'

# rate limit 상황 — 초당 5건 초과 시 429
curl -X PUT http://localhost:8081/config \
  -H "Content-Type: application/json" \
  -d '{"rateLimitPerSecond":5}'

# 단건 발송 테스트
curl -X POST http://localhost:8081/send \
  -H "Content-Type: application/json" \
  -d '{"messageId":"msg-0001","recipientId":"user-123","channel":"SMS","content":"테스트"}'
```

## 참고

- 이 모듈은 v1과 v2에서 **동일하게 사용**됩니다. 두 버전을 같은 조건에서 비교하기 위함이므로, v2 전환 시에도 수정하지 않습니다.
- 설정값은 메모리에만 보관되며 재시작하면 기본값으로 돌아갑니다.
- 프로젝트 전체 설명은 [루트 README](../README.md)를 참고하세요.