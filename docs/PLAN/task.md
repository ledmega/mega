# OS 모니터링 관리 기능 개발 태스크

- [x] **Phase 1: Database & Entity (기반 마련)**
    - [x] `os_monitoring_config` 테이블 생성 (DDL 작성)
    - [ ] `OsMonitoringConfig` 엔티티 및 `OsMonitoringConfigRepository` 생성
- [ ] **Phase 2: Backend API (서버 로직)**
    - [ ] `OsMonitoringConfigService` 구현 (CRUD 및 에이전트용 조회)
    - [ ] `OsMonitoringConfigApiController` 구현
    - [ ] 에이전트 전용 설정 Pull API 엔드포인트 추가
- [ ] **Phase 3: Frontend UI (관리 화면)**
    - [ ] OS 모니터링 관리 메뉴 및 목록 화면 구현
    - [ ] 설정 등록/수정/삭제 모달(팝업) 구현
    - [ ] 대시보드 연동 (dashboard_yn 필드에 따른 필터링)
- [ ] **Phase 4: Agent Integration (에이전트 연동)**
    - [ ] 에이전트 내 `OsConfig` 모델 추가 및 주기적 Pull 로직 구현
    - [ ] 에이전트 `TaskScheduler` 동적 재구성 로직 적용 (주기 및 명령어 반영)
    - [ ] 프로세스 이상 감지 필터 알고리즘 적용
- [ ] **Phase 5: Verification (검증)**
    - [ ] 웹 설정 변경 시 에이전트 실시간 반영 테스트
    - [ ] 임계값(Threshold) 필터링 정상 작동 확인
