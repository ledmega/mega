# 권한(역할) 관리 기능

> **문서 유지**: 권한관리 관련 URL, 권한, 서비스/컨트롤러, 템플릿을 수정할 때 이 문서도 함께 반영해 주세요.

---

## 1. 개요

회원의 **역할(Role)** 을 조회하고 변경하는 기능입니다. **관리자(ROLE_ADMIN)** 만 접근할 수 있습니다.

- 역할별 회원 수 통계
- 역할별 회원 목록 (페이징)
- 목록에서 바로 역할 변경 (일반 ↔ 관리자)

---

## 2. URL 및 권한

| URL | 메서드 | 설명 | 권한 |
|-----|--------|------|------|
| `/authority` | GET | 권한관리 메인 (역할별 통계 + 회원 목록/필터) | **관리자만** |
| `/authority/members/{id}/role` | POST | 회원 역할 변경 | **관리자만** |

- 쿼리: `roleFilter`(ROLE_USER / ROLE_ADMIN), `page`, `size`(페이징)
- 비관리자 접근 시 `/dashboard` 로 리다이렉트

---

## 3. 보안

- `/authority`, `/authority/**` 는 별도 `permitAll` 없음 → 로그인 필수.
- `AuthorityController` 에서 `isAdmin(auth)` 로 체크 후, 비관리자는 `redirect:/dashboard`.

---

## 4. 코드 구조

### 4.1 Repository

- **MemberRepository**
  - `long countByRole(MemberRole role)` : 역할별 회원 수
  - `Page<Member> findByRoleOrderByCreatedAtDesc(MemberRole role, Pageable pageable)` : 역할별 목록

### 4.2 Service

- **MemberService**
  - `Map<MemberRole, Long> getMemberCountByRole()` : 역할별 인원 수
  - `Page<MemberDetailDto> getMembersByRole(MemberRole role, Pageable pageable)` : 역할별 회원 목록
  - `MemberDetailDto updateRole(Long id, MemberRole role)` : 역할 변경

### 4.3 Controller

- **AuthorityController** (`/authority`)
  - `GET /authority` : 역할별 통계 + 필터(전체/역할별)에 따른 회원 목록
  - `POST /authority/members/{id}/role` : 역할 변경 (request param: `role`)

### 4.4 엔티티/역할

- **MemberRole** (enum): `ROLE_USER`, `ROLE_ADMIN`

### 4.5 템플릿

| 경로 | 용도 |
|------|------|
| `templates/authority/list.html` | 권한관리 메인 (통계 카드, 필터, 회원 테이블, 역할 변경 폼) |

---

## 5. 네비게이션

- **대시보드**, **홈**, **회원 목록/상세/수정**: 관리자일 때만 **권한관리** (`/authority`) 링크 노출.
- **권한관리** 페이지 상단 네비: 홈, 대시보드, 에이전트, 회원관리, 권한관리(강조), 로그아웃.

---

## 6. 수정 시 체크리스트

권한관리 기능을 수정할 때 아래를 함께 확인·수정하면 좋습니다.

- [ ] URL/메서드 추가·변경 시 → 이 문서 **2. URL 및 권한** 반영
- [ ] 권한 로직 변경 시 → **3. 보안** 반영
- [ ] Repository/Service/Controller 변경 시 → **4. 코드 구조** 해당 절 반영
- [ ] 역할(enum) 추가·변경 시 → **4.4**, 템플릿 선택 옵션 반영
- [ ] 메뉴/링크 변경 시 → **5. 네비게이션** 반영
