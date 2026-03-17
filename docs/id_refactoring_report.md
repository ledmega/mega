# 시스템 전면 ID 체계 개편 및 DB 초기화 오류 해결 보고서

본 문서는 시스템의 ID 체계를 숫자형(Long)에서 문자형(String) 프리픽스 기반 체계로 전면 개편한 내용과, 그 과정에서 발생한 데이터베이스 초기화 오류를 해결한 내역을 정리한 보고서입니다.

## 1. 개요
기존의 숫자형 자동 증가(Auto-increment) ID 체계를 폐지하고, 각 도메인별 고유 식별자(Prefix)를 포함한 문자열 ID 체계로 전환하였습니다. 이를 통해 에이전트와 서버 간의 데이터 식별성을 높이고 분산 환경에서의 ID 충돌 가능성을 최소화하였습니다.

## 2. 주요 변경 사항

### 2.1 에이전트 (Agent)
- **ID 타입 변경**: 모든 DTO 및 내부 로직의 ID 타입을 `Long`에서 `String`으로 변경하였습니다.
- **인증 방식 유지**: API Key 기반 인증 로직은 유지하되, 에이전트 등록 후 발급받는 `agentDbId`를 문자열로 처리하도록 수정하였습니다.
- **설정 로드/저장**: `.agent_id` 파일에 저장되는 `agentDbId` 정보를 문자열로 처리하도록 `ApiClient` 및 `AgentApplication`을 수정하였습니다.

### 2.2 웹 서버 (Web Server)
- **엔티티 개편**: 모든 주요 엔티티의 `@Id` 필드를 `String`으로 변경하고, Spring Data R2DBC의 `Persistable<String>` 인터페이스를 구현하였습니다.
    - 이는 수동으로 할당된 문자열 ID를 가진 엔티티를 `save()`할 때, R2DBC가 기존 데이터로 오인하여 `UPDATE`를 시도하는 대신 `INSERT`를 수행하도록 강제하기 위함입니다.
- **대상 엔티티**: `Agent`, `Member`, `Menu`, `MonitoringConfig`, `Task`, `BatchJob`, `MetricData`, `CsFaq`, `CsConversation`, `CsInboundData`, `CsMessage` 등 전체 엔티티.

### 2.3 데이터베이스 초기화 (Database Initialization)
- **SQL 수정**: `schema.sql` 및 `DatabaseInitializer` 내의 `INSERT` 쿼리를 수정하여 `menu_id`, `batch_job_id` 등 수동 할당 ID를 포함하도록 하였습니다.
- **기본 메뉴 초기화**: `MenuService` 및 `DatabaseInitializer`에서 기본 메뉴 생성 시 문자열 형태의 ID(`MNU0000000001` 등)를 명시적으로 부여합니다.

## 3. 발생 오류 및 해결 내역

| 오류 메시지 | 원인 | 해결 방법 |
| :--- | :--- | :--- |
| `Field 'menu_id' doesn't have a default value` | `menu` 테이블의 `menu_id`가 자동 증가가 아닌데 INSERT 시 값이 누락됨 | `DatabaseInitializer`의 SQL 문에 `menu_id` 값을 추가함 |
| `Row with Id [BTJ...] does not exist` | R2DBC가 수동 할당 ID를 보고 UPDATE를 시도함 | `BatchJob` 엔티티에 `Persistable`을 구현하고 `isNew` 플래그를 도입함 |
| `OpenAI API key must be set` | Spring AI 설정 중 API Key가 누락되어 구동 실패 | `application.properties`에 더미 API Key 설정을 추가함 |

## 4. 적용된 ID 프리픽스 규칙
- `MNU`: 메뉴 (Menu)
- `BTJ`: 배치 작업 (Batch Job)
- `AGT`: 에이전트 (Agent)
- `MEM`: 회원 (Member)
- `CFG`: 모니터링 설정 (Monitoring Config)
- `TSK`: 태스크 (Task)
- `MET`: 메트릭 데이터 (Metric Data)
- `FAQ`: 자주 묻는 질문 (CS FAQ)
- `CNV`: CS 대화 (CS Conversation)
- `MSG`: CS 메시지 (CS Message)
- `IBD`: CS 인바운드 데이터 (CS Inbound Data)

## 5. 향후 계획
- **실제 API Key 연동**: 현재 더미로 설정된 OpenAI API Key를 실제 환경 변수(`OPENAI_API_KEY`)로 교체하여 운영이 필요합니다.
- **린트 에러 정리**: ID 타입 변경으로 인해 발생한 일부 타입 안전성(Null safety) 경고들에 대해 지속적인 리팩토링을 진행할 예정입니다.
