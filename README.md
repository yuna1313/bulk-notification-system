# bulk-notification-system

대량 알림 발송 과정에서 발생하는 동기 처리 구조의 한계를 직접 측정하고, 이를 Kafka 기반 비동기 구조로 개선한 프로젝트입니다.

먼저 수신자를 한 명씩 순차 발송하는 v1을 구현해 부하 테스트를 진행했습니다.
이후 v1에서 확인한 문제를 기준으로 Kafka 기반 비동기 구조의 v2를 구현하고 같은 조건에서 다시 측정했습니다.

---

## 프로젝트 구성

| 모듈                  | 역할                                               |
| --------------------- | -------------------------------------------------- |
| `notification-api`    | 알림 접수, 발송 요청 및 현황 조회                  |
| `notification-worker` | Outbox 이벤트 발행, Kafka 메시지 소비 및 실제 발송 |
| `mock-provider`       | 외부 발송사를 대신하는 테스트 서버                 |
| `load-test`           | k6 부하 테스트                                     |
| `docs`                | v1 / v2 상세 테스트 결과                           |

`mock-provider`에서는 응답 지연시간, 실패율, Rate Limit을 조절해 외부 발송사의 지연이나 실패 상황을 재현할 수 있습니다.

---

## 구조 변화

### v1

```text
Client
  ↓
notification-api
  ↓
수신자 순차 발송
  ↓
mock-provider
```

API 서버가 실제 발송까지 직접 처리합니다.

부하 테스트를 통해 순차 처리로 인한 낮은 처리량, 외부 API 지연 전파, Tomcat Thread 장시간 점유 등의 문제를 확인했습니다.

### v2

```text
Client
  ↓
notification-api
  ↓
outbox_event
  ↓
Poller
  ↓
Kafka
  ↓
Consumer
  ↓
mock-provider
```

실제 발송을 `notification-worker`로 분리하고 Kafka Consumer를 통해 여러 메시지를 병렬 처리하도록 변경했습니다.

추가로 Transactional Outbox, 중복 처리 방지, Retry / DLQ를 적용했습니다.

---

## 주요 기술

- Java 17
- Spring Boot
- Spring Data JPA
- MySQL 8
- Apache Kafka
- Transactional Outbox
- Retry / DLQ
- k6
- Docker

---

## 핵심 결과

| 항목                |          v1 |             v2 |
| ------------------- | ----------: | -------------: |
| 100,000건 발송시간  |  6시간 19분 |  **48분 56초** |
| 발송 처리량         | 약 4.4건/초 | **34.06건/초** |
| 100,000건 최종 실패 |     2,974건 |        **1건** |

10만 건 기준 발송시간은 약 **7.7배 단축**됐습니다.

상세한 측정 조건과 결과 분석은 별도 문서에 정리했습니다.

- [v1 부하 테스트 결과](./docs/load-test-v1.md)
- [v2 부하 테스트 결과](./docs/load-test-v2.md)

부하 테스트 실행 및 재현 방법은 [`load-test/README.md`](./load-test/README.md)에서 확인할 수 있습니다.
