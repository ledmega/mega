## 권한(역할) 설계 개요

> 이 문서는 **현재 구현된 권한 구조를 정리**하고,  
> 앞으로 권한 관련 기능을 확장/리팩터링할 때 참고할 **개념·설계 가이드**입니다.  
> 구현 상세는 다음 문서를 함께 참고하세요.
>
> - [`docs/AUTHORITY_MANAGEMENT.md`](AUTHORITY_MANAGEMENT.md) – 권한(역할) 관리 기능 설명
> - [`docs/MEMBER_MANAGEMENT.md`](MEMBER_MANAGEMENT.md) – 회원 관리 기능 설명

---

### 1. 현재 역할/사용자 모델

- **MemberRole (역할)**
  - `ROLE_USER`
    - 일반 사용자 (기본 회원)
    - 자신의 정보 조회/수정, 대시보드 접근
  - `ROLE_ADMIN`
    - 관리자
    - 회원관리, 권한관리, 시스템 모니터링 기능 접근

- **MemberStatus (회원 상태)**
  - `ACTIVE` – 정상 사용 중
  - (기타 상태는 코드/DB 정의 참고)

- **사용자 유형**
  - **웹 사용자 (Member)**
    - 이메일/비밀번호로 로그인
    - `ROLE_USER` 또는 `ROLE_ADMIN` 보유
  - **에이전트 (Agent)**
    - API 키로 인증
    - Spring Security 상에서 `ROLE_AGENT` 권한으로 동작 (ApiKeyAuthenticationFilter)
    - `/api/agents/**` 계열 엔드포인트에 접속

---

### 2. 리소스(기능)별 접근 정책 설계

아래 표는 **주요 URL 그룹별로 어떤 역할이 어떤 액션을 할 수 있는지**를 정리한 것입니다.  
현재 구현 상태 + 향후 확장 아이디어를 함께 반영했습니다.

#### 2.1 요약 매트릭스

| 리소스/기능 | URL 예시 | 액션 | ROLE_ANONYMOUS (비로그인) | ROLE_USER | ROLE_ADMIN | ROLE_AGENT |
|------------|---------|------|---------------------------|----------|-----------|-----------|
| 홈/로그인/회원가입 | `/`, `/login`, `/signup` | 조회/접근 | ✅ | ✅ | ✅ | ❌ |
| 대시보드 | `/dashboard` | 조회 | ❌ (리다이렉트/로그인 요구) | ✅ | ✅ | ❌ |
| 내 정보 | `/members/me` | 조회/리다이렉트 | ❌ | ✅ (자기 자신만) | ✅ | ❌ |
| 회원 목록 | `/members` | 조회 | ❌ | ❌ | ✅ | ❌ |
| 회원 상세/수정 | `/members/{id}`<br>`/members/{id}/edit` | 조회/수정 | ❌ | ✅ (자기 자신만) | ✅ | ❌ |
| 회원 상태 변경 | `/members/{id}/status` | 상태 변경 | ❌ | ❌ | ✅ | ❌ |
| 권한관리 메인 | `/authority` | 역할 통계/목록 조회 | ❌ | ❌ | ✅ | ❌ |
| 역할 변경 | `/authority/members/{id}/role` | 역할 변경 | ❌ | ❌ | ✅ | ❌ |
| 에이전트 대시보드(웹) | `/agents`, `/agents/{id}` | 조회 | ❌ | ✅ | ✅ | ❌ |
| API – 에이전트 등록 | `/api/agents/register` | 등록 | ✅ (공개) | ✅ | ✅ | ✅ |
| API – 하트비트/메트릭/예외 | `/api/agents/{agentId}/...` | 생성 | ❌ | ❌ | ❌ | ✅ (API 키) |
| SSE 스트림 | `/api/sse/events` | 구독 | ❌ | ✅ | ✅ | ❌ |

> 실제 코드는 `SecurityConfig` 와 각 Controller 의 `isAdmin`, `canAccessMember` 로 구현되어 있으며,  
> 위 표는 그 정책을 설계 관점에서 요약한 것입니다.

---

### 3. 도메인 모델과 권한의 연결

#### 3.1 Member / MemberRole / MemberStatus

- `Member`
  - 역할: 웹 사용자 계정
  - 필드: `email`, `password`, `name`, `nickname`, `phone`, `role`, `status`, `lastLoginAt`, `createdAt`, `updatedAt` 등
  - 역할/상태는 관리 화면(회원관리/권한관리)에서 관리자가 변경 가능

- `MemberRole`
  - 단순 Enum 기반 역할
  - 장점: 구현이 단순하고 Security 설정과 매핑이 쉽다.
  - 한계: 역할이 늘어나면 Enum이 비대해지고, 세밀한 권한 조합을 표현하기 어렵다.

- `MemberStatus`
  - 로그인 허용 여부, 화면 노출 등을 결정
  - 예: `ACTIVE` 가 아니면 `CustomUserDetailsService` 에서 로그인 실패 처리

#### 3.2 Agent / API Key / ROLE_AGENT

- `Agent`
  - 모니터링 대상 호스트/에이전트
  - `apiKey` 필드로 인증

- `ApiKeyAuthenticationFilter`
  - HTTP 헤더 `Authorization: Bearer {apiKey}` 를 읽어서 `Agent` 를 찾는다.
  - 성공 시 `UsernamePasswordAuthenticationToken` 에 `ROLE_AGENT` 권한을 부여하여 Security Context 에 저장.

- 설계 아이디어
  - **에이전트용 세분화된 권한**을 도입하고 싶다면:
    - 예: `AGENT_METRIC_WRITE`, `AGENT_EXCEPTION_WRITE` 등
    - 현재는 단순히 \"API 키가 유효하면 해당 Agent 가 모든 /api/agents/** 를 호출 가능\"한 구조.

---

### 4. SecurityConfig 차원 설계

#### 4.1 경로 기반 접근 정책

`SecurityConfig` (WebFlux) 에서는 `authorizeExchange` + `pathMatchers` 로 URL 별 정책을 정의합니다.

```java
.authorizeExchange(auth -> auth
    .pathMatchers(
        "/", "/public/**", "/css/**", "/js/**", "/images/**",
        "/favicon.ico", "/signup", "/login", "/dashboard", "/agents"
    ).permitAll()
    .pathMatchers("/api/agents/register").permitAll()
    .pathMatchers("/api/**").authenticated()
    .anyExchange().authenticated()
)
```

- **익명 허용**: 홈, 정적 리소스, 로그인/회원가입, 대시보드(초기 접근) 등
- **API 등록만 공개**: `/api/agents/register` (에이전트 최초 등록)
- **기타 API**: `/api/**` 는 인증 필요 (Agent API 키 또는 로그인 사용자)
- **나머지**: 일반 웹 페이지는 로그인 필요, 세부 권한은 Controller 단에서 `isAdmin`, `canAccessMember` 로 판별

#### 4.2 Controller 레벨 권한 체크

- `MemberController`
  - `isAdmin(Authentication)` – 관리자 여부 체크
  - `canAccessMember(Long memberId, Authentication)` – 관리자이거나 본인인지 체크

- `AuthorityController`
  - `isAdmin(Authentication)` – 관리자만 접근 허용, 아니면 `/dashboard` 로 리다이렉트

이 패턴을 유지하면:

- **SecurityConfig**: \"인증 여부\" 와 큰 URL 그룹 수준의 정책만 담당
- **Controller/Service**: 도메인 지식을 이용한 세밀한 권한 판단 담당

---

### 5. 권한관리 UI/UX 구상

#### 5.1 상단 네비게이션 시나리오

- **비로그인 사용자**
  - 메뉴: 홈, 로그인, 회원가입 정도만 노출
  - `/members/**`, `/authority/**`, `/dashboard` 접근 시 로그인 페이지로 유도

- **일반 사용자 (ROLE_USER)**
  - 메뉴 예시:
    - 홈
    - 대시보드
    - 내 정보 (`/members/me`)
    - 에이전트(읽기 전용) – 필요 시
  - 권한관리(`/authority`), 회원관리(`/members`) 링크는 노출하지 않거나 접근 시 `/dashboard` 로 리다이렉트

- **관리자 (ROLE_ADMIN)**
  - 메뉴 예시:
    - 홈
    - 대시보드
    - 에이전트 목록
    - 회원관리 (`/members`)
    - 권한관리 (`/authority`)
    - 로그아웃

#### 5.2 권한관리 화면(Authority)

- 상단: 역할별 통계 카드 (전체 사용자 수, ROLE_USER 수, ROLE_ADMIN 수 등)
- 가운데: 역할 필터(전체/USER/ADMIN) + 회원 목록
  - 각 행에 역할 변경 셀렉트 박스 또는 버튼
  - 변경 시 POST `/authority/members/{id}/role`
- 향후 확장 아이디어:
  - 역할별 기본 권한 목록을 보여주는 섹션
  - 일괄 변경, 감사 로그 버튼 등

#### 5.3 회원관리 화면(Members)

- 목록 화면:
  - 검색(이메일/이름) + 결과 테이블
  - 관리자일 때만 접근 가능
- 상세/수정 화면:
  - 본인은 자신의 정보만 접근 가능
  - 관리자는 모든 회원 접근/수정 가능
  - 관리자에게만 역할/상태 변경 UI 노출

---

### 6. 확장/리팩터링 아이디어

#### 6.1 세분화된 권한 시스템 (역할 + 권한 조합)

현재는 **단일 Enum 기반 역할(Role)만 존재**합니다.

- 장점
  - 구현이 단순하고 이해하기 쉽다.
  - 작은 프로젝트/팀에서는 유지보수가 편하다.
- 단점
  - 역할 종류가 많아지면 Enum 이 비대해진다.
  - \"읽기 전용 관리자\", \"모니터링 전용 계정\" 같은 세밀한 요구를 표현하기 어렵다.

확장 방향 예시:

- DB 레벨 권한 모델 도입
  - `role(id, name, description)`
  - `permission(id, name, description)`
  - `role_permission(role_id, permission_id)`
  - `member_role(member_id, role_id)`
- 어플리케이션 상에서:
  - `@PreAuthorize(\"hasAuthority('MANAGE_MEMBER')\")` 와 같이 메서드 단 권한 체크 도입
  - Controller/service 메서드에 도메인 중심의 권한 명시

#### 6.2 Controller 수준 권한 체크 정리

현재는 `isAdmin`, `canAccessMember` 같은 메서드로 권한을 수동 체크하고 있습니다.  
향후에는 다음과 같은 방향으로 리팩터링할 수 있습니다.

- Spring Security의 메서드 보안 활성화
  - `@EnableMethodSecurity` 또는 WebFlux용 메서드 보안
  - 예: `@PreAuthorize(\"hasRole('ADMIN')\")`, `@PreAuthorize(\"#id == principal.id or hasRole('ADMIN')\")`
- 장점
  - 권한 규칙이 메서드에 붙어 있어 가독성이 높음
  - 테스트/검증이 쉬워짐

---

### 7. 운영/관리 관점 체크리스트

새로운 기능이나 URL 을 추가할 때, 아래 질문들을 체크하면 권한 설계를 놓치지 않을 수 있습니다.

1. **누가 이 기능을 써야 하는가?**
   - 익명 사용자도 가능한가?
   - 로그인 사용자 전체인가?
   - 관리자만 가능한가?
   - 에이전트(API 키)만 가능한가?

2. **읽기 vs 쓰기 권한이 다른가?**
   - 읽기는 USER, 쓰기는 ADMIN 이어야 하는가?
   - 삭제/상태 변경은 더 강한 권한이 필요한가?

3. **자기 자신만 접근 가능한가?**
   - URL 에 `/{id}` 가 포함될 때, \"본인만\" 허용해야 하는 경우인가?
   - 그렇다면 `canAccessXXX` 같은 헬퍼를 만들거나, 메서드 보안 표현식으로 처리할 수 있는가?

4. **로그/감사 용도는 필요한가?**
   - 역할/상태 변경, 중요 설정 변경에 대해 누가 언제 무엇을 했는지 로그가 필요한가?
   - 향후 감사 로그 화면을 만들 계획이 있다면 스키마/서비스 설계 시점부터 고려해야 한다.

5. **UI 에서 권한을 어떻게 표현할 것인가?**
   - 일반 사용자에게 보이면 안 되는 메뉴/버튼이 있는가?
   - \"비활성화된 버튼 + 툴팁\" vs \"아예 안 보이게\" 중 어떤 UX 를 선택할 것인가?

6. **API 호환성**
   - 권한 정책 변경 시, 기존 클라이언트(에이전트/프론트엔드)가 영향을 받지 않는가?
   - API 응답 코드(401/403/404 등)를 어떻게 사용할지 일관되게 설계했는가?

---

### 8. 문서 유지 가이드

이 문서는 **설계/정책 레벨** 문서이며, 구현이 바뀔 때 같이 업데이트해야 합니다.

- [ ] 역할 Enum(MemberRole) 추가/변경 시 – 1장, 3장 내용 반영
- [ ] SecurityConfig 의 URL 정책 변경 시 – 2장, 4장 매트릭스/설명 반영
- [ ] 회원/권한 관련 컨트롤러(AuthorityController, MemberController) 변경 시 – 2장, 5장 수정
- [ ] 권한 모델을 DB 기반으로 확장할 경우 – 6장 내용을 실제 설계에 맞게 업데이트
- [ ] 새로운 관리자/모니터링 화면 추가 시 – 2장, 5장에 리소스/권한 설계 추가

