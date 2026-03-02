# 회원관리 기능

> **문서 유지**: 회원관리 관련 URL, 권한, 서비스/컨트롤러, 템플릿을 수정할 때 이 문서도 함께 반영해 주세요.

---

## 1. URL 및 권한

| URL | 메서드 | 설명 | 권한 |
|-----|--------|------|------|
| `/members` | GET | 회원 목록 (페이징, 이메일/이름 검색) | **관리자만** |
| `/members/me` | GET | 내 정보(현재 로그인 회원 상세로 리다이렉트) | 로그인 사용자 |
| `/members/{id}` | GET | 회원 상세 | 관리자 또는 **본인** |
| `/members/{id}/edit` | GET | 회원 수정 폼 | 관리자 또는 본인 |
| `/members/{id}/edit` | POST | 회원 수정 처리 | 관리자 또는 본인 |
| `/members/{id}/status` | POST | 상태 변경 (활성/비활성/탈퇴) | **관리자만** |

- **관리자**: `MemberRole.ROLE_ADMIN` 보유 시 회원 목록·상태 변경 가능.
- **본인**: 로그인한 회원과 동일한 `id`인 경우 상세·수정 접근 가능.
- 역할 변경: 회원 수정 폼(관리자)에서 가능하며, **권한관리** (`/authority`) 에서 역할별 조회·일괄 변경 가능. → [권한관리 문서](AUTHORITY_MANAGEMENT.md) 참고.

---

## 2. 보안 설정

- `/members`, `/members/**` 는 `SecurityConfig` 에서 별도 `permitAll` 하지 않음 → `anyRequest().authenticated()` 로 **로그인 필수**.
- 목록·상태 변경은 컨트롤러에서 `isAdmin(auth)` 로 체크 후, 비관리자는 `/dashboard` 로 리다이렉트.
- 상세·수정은 `canAccessMember(id, auth)` 로 관리자 또는 본인만 접근 허용.

---

## 3. 코드 구조

### 3.1 Repository

- **MemberRepository**
  - `findByEmailContainingOrNameContaining(String email, String name, Pageable pageable)`  
    → 목록 검색(이메일 또는 이름) + 페이징.

### 3.2 DTO

- **MemberDetailDto**  
  회원 상세/목록 노출용. 비밀번호 제외.  
  `id`, `email`, `name`, `nickname`, `phone`, `role`, `status`, `createdAt`, `updatedAt`, `lastLoginAt`.  
  `MemberDetailDto.from(Member)` 정적 메서드 제공.
- **MemberUpdateDto**  
  수정 폼 바인딩.  
  `name`, `nickname`, `phone`, `role`, `status`.  
  역할/상태는 관리자일 때만 서비스에서 반영.

### 3.3 Service

- **MemberService**
  - `getMemberPage(String search, Pageable pageable)` → `Page<MemberDetailDto>`
  - `getMemberDetail(Long id)` → `MemberDetailDto`
  - `updateMember(Long id, MemberUpdateDto dto, boolean isAdmin)` → `MemberDetailDto`
  - `updateStatus(Long id, MemberStatus status)` → `MemberDetailDto` (관리자 전용)

### 3.4 Controller

- **MemberController**
  - 회원가입: `GET/POST /signup` (기존).
  - 회원관리: 위 1장 URL 표와 동일.
  - `isAdmin(Authentication)`, `canAccessMember(Long memberId, Authentication)` 로 권한 판단.

### 3.5 템플릿

| 경로 | 용도 |
|------|------|
| `templates/members/list.html` | 회원 목록, 검색, 페이징 |
| `templates/members/detail.html` | 회원 상세, 수정/목록/상태변경 링크 |
| `templates/members/edit.html` | 회원 수정 폼 (관리자일 때 역할/상태 선택 노출) |

---

## 4. 네비게이션

- **대시보드** (`dashboard.html`): 로그인 시 **내 정보** (`/members/me`), 관리자일 때 **회원관리** (`/members`).
- **홈** (`index.html`): 로그인 시 **내 정보**, **회원관리**(관리자만), 카드 링크로 동일 URL 제공.
- 회원관리 전용 페이지(목록/상세/수정) 상단 네비: 홈, 대시보드, 에이전트, 회원관리, 로그아웃.

---

## 5. 관리자 계정

회원 목록·상태 변경은 `ROLE_ADMIN` 만 가능. 테스트용 관리자는 DB에서 역할 변경:

```sql
UPDATE ledmega.member SET role = 'ROLE_ADMIN' WHERE email = 'your@email.com';
```

해당 이메일로 로그인 후 **회원관리** 메뉴와 `/members` 목록 이용 가능.

---

## 6. 수정 시 체크리스트

회원관리 기능을 수정할 때 아래를 함께 확인·수정하면 좋습니다.

- [ ] URL/메서드 추가·변경 시 → 이 문서 **1. URL 및 권한** 반영
- [ ] 권한 로직 변경 시 → **2. 보안 설정** 및 **3.4 Controller** 설명 반영
- [ ] Repository/Service/DTO 변경 시 → **3. 코드 구조** 해당 절 반영
- [ ] 새 템플릿 또는 경로 변경 시 → **3.5 템플릿** 반영
- [ ] 메뉴/링크 변경 시 → **4. 네비게이션** 반영
- [ ] 관리자 정책 변경 시 → **5. 관리자 계정** 반영
