# MEGA 프로젝트 ID 체계 및 아키텍처 전면 개편 안내

이 문서는 프로젝트의 확장성과 가독성을 높이기 위해 수행된 **ID 체계 변경(Long -> String Prefix ID)** 및 이에 따른 전체 레이어 리팩토링 내용을 정리한 문서입니다.

## 1. 개요 및 목적
기존의 `BIGINT AUTO_INCREMENT` 방식의 ID는 단순 숫자로 구성되어 어떤 테이블의 데이터인지 파악하기 어렵고, 분산 환경에서의 확장에 제약이 있었습니다. 이를 개선하기 위해 각 테이블별로 고유한 Prefix를 가진 15자리 문자열 ID 체계를 도입하였습니다.

## 2. 주요 변경 사항

### 2.1 ID 생성 전략 (IdGenerator)
새로운 ID는 `Prefix(3자) + 10자리 숫자`로 구성됩니다. (총 13~15자 내외)
- **예시**: `MBR0000000001`, `AGT001710562479`, `BTJ0000000005`
- **구현**: `led.mega.util.IdGenerator` 클래스에서 중앙 집중식으로 생성합니다.

| 테이블 | Prefix | 예시 |
| :--- | :--- | :--- |
| `member` | `MBR` | `MBR0000000001` |
| `agent` | `AGT` | `AGT0000000001` |
| `task` | `TSK` | `TSK0000000001` |
| `metric_data` | `MET` | `MET0000000001` |
| `exception_log` | `EXL` | `EXL0000000001` |
| `agent_heartbeat` | `HBT` | `HBT0000000001` |
| `menu` | `MNU` | `MNU0000000001` |
| `monitoring_config` | `CFG` | `CFG0000000001` |
| `batch_job` | `BTJ` | `BTJ0000000001` |
| `service_metric_data` | `SMT` | `SMT0000000001` |
| `cs_faq` | `FAQ` | `FAQ0000000001` |
| `cs_conversation` | `CON` | `CON0000000001` |
| `cs_message` | `MSG` | `MSG0000000001` |
| `cs_inbound_data` | `INB` | `INB0000000001` |

### 2.2 Database (DDL)
- 모든 Primary Key 컬럼의 타입을 `BIGINT`에서 `VARCHAR(50)`로 변경하였습니다.
- 컬럼명을 테이블명을 포함하도록 명확히 변경하였습니다. (예: `id` -> `batch_job_id`)
- Foreign Key 참조 타입 또한 `VARCHAR(50)`로 일괄 업데이트하였습니다.
- 관련 파일: `schema.sql`, `cs_ai_tables.sql`

### 2.3 Java Entity 및 Repository
- `@Id` 필드의 타입을 `Long`에서 `String`으로 변경하였습니다.
- R2DBC 연동을 위해 `@Column` 애노테이션으로 매핑된 테이블 컬럼명을 새로운 규칙에 맞게 수정하였습니다.
- Repository 인터페이스의 ID 타입을 `Long`에서 `String`으로 변경하고, 메서드 시그니처를 업데이트하였습니다.

### 2.4 Service 및 DTO 레이어
- **아이디 생성**: 새로운 엔티티 생성 시 `IdGenerator.generate()`를 호출하여 ID를 수동 할당합니다.
- **DTO 전송**: 클라이언트와 주고받는 모든 ID 필드를 `String`으로 통합하였습니다.
- **비즈니스 로직**: `agentId` 등 외래키 참조 시에도 String 타입을 일관되게 사용하도록 리팩토링하였습니다.

### 2.5 Controller (API & View)
- `@PathVariable` 및 `@RequestParam`으로 전달되는 ID 파라미터 타입을 `String`으로 변경하였습니다.
- 에이전트 인증 로직(`getAuthenticatedAgentMono`)에서 물리적 ID(`agentId`)와 논리적 ID(`agentRefId`)를 모두 지원하도록 개선하였습니다.

## 3. 리팩토링 수행 이력 (수정된 파일 목록)
- **Entities**: `Member`, `Agent`, `Task`, `MetricData`, `ExceptionLog`, `AgentHeartbeat`, `Menu`, `MonitoringConfig`, `BatchJob`, `ServiceMetricData` 및 CS 관련 엔티티 전체
- **Repositories**: 위 엔티티에 대응하는 모든 `Repository` 인터페이스
- **DTOs**: `MemberDetailDto`, `AgentResponseDto`, `TaskResponseDto`, `MetricDataRequestDto`, `ExceptionLogRequestDto`, `WebSocketMessageDto` 등
- **Services**: `MemberService`, `AgentService`, `TaskService`, `MetricDataService`, `ExceptionLogService`, `MonitoringConfigService`, `BatchJobService`, `SseService` 등
- **Controllers**: `AgentApiController`, `TaskApiController`, `MemberController`, `MenuController`, `BatchJobApiController` 등

## 4. 향후 과제 및 주의사항
1. **프론트엔드 호환성**: API 응답의 ID가 숫자가 아닌 문자열로 오므로, JS 단에서 `parseInt()` 등을 사용하던 로직이 있다면 수정이 필요합니다.
2. **기존 데이터 마이그레이션**: 운영 환경의 경우 기존 Long ID를 String Prefix ID로 변환하는 마이그레이션 스크립트가 필요합니다. (현재는 테이블 드롭 후 재생성 방식 사용)
3. **테스트 코드 업데이트**: 기존에 `Long` ID를 하드코딩했던 유닛 테스트 및 통합 테스트 코드를 문자열 ID로 변경해야 합니다.

---
*본 문서는 2024년 5월 ID 리팩토링 작업 완료 후 작성되었습니다.*
