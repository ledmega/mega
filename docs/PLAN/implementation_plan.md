# OS 모니터링 동적 설정 기능 구현 계획

본 계획서는 에이전트의 OS 자원 수집 설정(CPU, 메모리, 디스크 등)을 웹 서버에서 동적으로 관리하기 위한 개발 로직을 담고 있습니다.

## Proposed Changes

### 1. Database & Domain (기반 마련)
#### [NEW] `os_monitoring_config` (Table)
- 에이전트별 OS 수집 항목과 주기를 저장하는 테이블을 생성합니다.
- `metric_type` (CPU, MEMORY, DISK, PROCESS), `interval_seconds`, `collect_yn`, `dashboard_yn`, `threshold_value` 등의 필드를 포함합니다.

#### [NEW] [OsMonitoringConfig.java](file:///e:/ws/mega/webserver/src/main/java/led/mega/entity/OsMonitoringConfig.java)
- R2DBC 전용 엔티티 클래스를 추가합니다.

#### [NEW] [OsMonitoringConfigRepository.java](file:///e:/ws/mega/webserver/src/main/java/led/mega/repository/OsMonitoringConfigRepository.java)
- 기본 CRUD 및 `agentId` 기반 조회를 위한 Repository를 추가합니다.

---

### 2. Backend API (서버 로직)
#### [NEW] `OsMonitoringConfigService.java`
- 에이전트가 설정을 요청할 때(Pull) 현재 활성 상태인 OS 설정 목록을 가공하여 반환하는 로직을 구현합니다.

#### [NEW] `OsMonitoringConfigApiController.java`
- 관리 화면에서 호출할 CRUD API 엔드포인트를 제공합니다.
- 예: `GET /api/os-configs`, `POST /api/os-configs` 등

#### [MODIFY] `MonitoringConfigApiController.java`
- 에이전트가 전체 설정을 가져갈 때 서비스 설정뿐만 아니라 OS 자원 설정도 함께 내려주도록 기존 `AgentConfig` 응답 스키마를 확장합니다.

---

### 3. Frontend UI (관리 화면)
#### [NEW] `os-config/list.html`
- OS 수집 항목별 주기, 수집 여부, 대시보드 표시 여부를 설정하는 관리 UI를 추가합니다.
- 부트스트랩 모달을 사용하여 설정 수정 기능을 구현합니다.

#### [MODIFY] `dashboard.html`
- `dashboard_yn` 값이 'N'인 메트릭은 대시보드 차트 생성 대상에서 제외하는 로직을 보완합니다.

---

### 4. Agent Integration (에이전트 연동)
#### [MODIFY] `TaskScheduler.java`
- 서버로부터 받은 OS 설정을 기반으로 기존 고정 주기의 작업을 취소하고, 새로운 주기에 맞춰 작업을 재등록하는 동적 스케줄링 기능을 구현합니다.

#### [MODIFY] `MetricParser.java`
- 수집된 데이터가 설정된 `threshold_value`를 초과할 때만 서버로 전송하도록 필터링 로직을 추가합니다.

---

## Verification Plan

### Automated Tests
- `OsMonitoringConfigService` 단위 테스트: 설정 데이터 조회 및 가공 로직 검증.
- API 통합 테스트: 에이전트 권한으로 OS 설정 수합 API 호출 결과 확인.

### Manual Verification
1. **웹 화면 테스트**: OS 모니터링 관리 메뉴에서 특정 항목(예: 디스크)의 주기를 10초에서 60초로 변경하고 저장합니다.
2. **에이전트 로그 확인**: 에이전트가 새로운 설정을 수신한 후, "디스크 수집 주기가 60초로 변경되었습니다"라는 로그가 출력되는지 확인합니다.
3. **대시보드 확인**: `dashboard_yn`을 'N'으로 변경했을 때, 해당 차트가 대시보드에서 사라지는지 실시간으로 검증합니다.
4. **임계값 테스트**: 디스크 사용량 임계치를 90%로 설정 후, 90% 미만인 상황에서 데이터 전송이 억제되는지 확인합니다.
