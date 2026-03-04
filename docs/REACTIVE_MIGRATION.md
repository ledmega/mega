# Spring WebFlux + R2DBC 리액티브 마이그레이션 가이드

> 브랜치 비교: `main` (블로킹) vs `feature/reactive` (논블로킹)

---

## 목차

1. [브랜치 비교 방법](#브랜치-비교-방법)
2. [의존성 변경 (build.gradle)](#의존성-변경)
3. [Entity 변환](#entity-변환)
4. [Repository 변환](#repository-변환)
5. [Service 변환 - 핵심 패턴](#service-변환---핵심-패턴)
6. [Security 변환](#security-변환)
7. [실시간 통신: WebSocket → SSE](#실시간-통신-websocket--sse)
8. [REST API Controller 변환](#rest-api-controller-변환)
9. [Thymeleaf Controller 변환](#thymeleaf-controller-변환)
10. [설정 파일 변경 (application.properties)](#설정-파일-변경)
11. [Mono/Flux 핵심 연산자 정리](#monoflux-핵심-연산자-정리)

---

## 브랜치 비교 방법

```bash
# 특정 파일 비교
git diff main feature/reactive -- webserver/src/main/java/led/mega/service/AgentService.java

# 전체 변경 요약
git diff --stat main feature/reactive

# GitHub에서 시각적으로 비교
# https://github.com/ledmega/mega/compare/main...feature/reactive
```

---

## 의존성 변경

### build.gradle

| 기존 (main) | 리액티브 (feature/reactive) | 이유 |
|------------|---------------------------|------|
| `spring-boot-starter-web` | `spring-boot-starter-webflux` | 서블릿 → Netty(논블로킹 서버) |
| `spring-boot-starter-data-jpa` | `spring-boot-starter-data-r2dbc` | JDBC(블로킹) → R2DBC(논블로킹) |
| `mariadb-java-client` | `r2dbc-mariadb` + `mariadb-java-client` | R2DBC 드라이버 추가, JDBC는 DatabaseInitializer용으로 유지 |
| `mybatis-spring-boot-starter` | (제거) | JPA와 함께 제거 |
| `spring-boot-starter-websocket` | (제거) | SSE로 교체 |
| `spring-boot-starter-web-services` | (제거) | 서블릿 기반 → 불필요 |

```groovy
// 기존
implementation 'org.springframework.boot:spring-boot-starter-web'
implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
implementation 'org.springframework.boot:spring-boot-starter-websocket'

// 리액티브
implementation 'org.springframework.boot:spring-boot-starter-webflux'
implementation 'org.springframework.boot:spring-boot-starter-data-r2dbc'
implementation 'org.mariadb:r2dbc-mariadb:1.2.2'
```

---

## Entity 변환

### 핵심 규칙

| 기존 (JPA) | 리액티브 (R2DBC) |
|-----------|----------------|
| `@Entity` | 불필요 (제거) |
| `@Table(name="...")` (jakarta) | `@Table("...")` (spring.data.relational) |
| `@Id` (jakarta) | `@Id` (spring.data.annotation) |
| `@GeneratedValue(IDENTITY)` | 불필요 (ID가 null이면 자동 INSERT) |
| `@Column(name="agent_id")` | 불필요 (camelCase → snake_case 자동 변환) |
| `@Enumerated(EnumType.STRING)` | 불필요 (R2DBC가 자동으로 name() 변환) |
| `@CreationTimestamp` (Hibernate) | `@CreatedDate` (spring.data.annotation) |
| `@UpdateTimestamp` (Hibernate) | `@LastModifiedDate` (spring.data.annotation) |
| `@ManyToOne Agent agent` | **`Long agentId`** (FK를 Long으로 단순화) |
| `@OneToMany List<Task> tasks` | **(제거)** R2DBC는 관계 매핑 미지원 |

### 코드 비교

```java
// 기존 (JPA)
@Entity
@Table(name = "agent_heartbeat")
public class AgentHeartbeat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    private Agent agent;           // 연관 객체 직접 참조

    @Enumerated(EnumType.STRING)
    private AgentStatus status;

    @CreationTimestamp
    private LocalDateTime createdAt;
}

// 리액티브 (R2DBC)
@Table("agent_heartbeat")
public class AgentHeartbeat {
    @Id
    private Long id;

    private Long agentId;          // FK를 Long으로 단순화

    private AgentStatus status;    // @Enumerated 불필요

    @CreatedDate
    private LocalDateTime createdAt;
}
```

> **주의**: R2DBC는 JOIN이나 관계 매핑을 지원하지 않는다.  
> 연관 데이터가 필요하면 서비스 레이어에서 별도로 조회해야 한다.

---

## Repository 변환

### 인터페이스 변경

| 기존 (JPA) | 리액티브 (R2DBC) |
|-----------|----------------|
| `JpaRepository<T, Long>` | `ReactiveCrudRepository<T, Long>` |
| `Optional<T>` | `Mono<T>` |
| `List<T>` | `Flux<T>` |
| `boolean` | `Mono<Boolean>` |
| `long` (count) | `Mono<Long>` |
| JPQL `@Query` | 네이티브 SQL `@Query` |
| `@Param("x")` | 파라미터명 직접 매핑 |

### 코드 비교

```java
// 기존 (JPA)
public interface AgentRepository extends JpaRepository<Agent, Long> {
    Optional<Agent> findByAgentId(String agentId);       // Optional
    boolean existsByAgentId(String agentId);              // boolean
    List<Agent> findByStatus(AgentStatus status);         // List

    @Query("SELECT a FROM Agent a WHERE a.lastHeartbeat < :threshold")  // JPQL
    List<Agent> findOfflineAgents(@Param("threshold") LocalDateTime threshold);

    @Query("SELECT COUNT(a) FROM Agent a WHERE a.status = :status")     // JPQL COUNT
    long countByStatus(@Param("status") AgentStatus status);
}

// 리액티브 (R2DBC)
public interface AgentRepository extends ReactiveCrudRepository<Agent, Long> {
    Mono<Agent> findByAgentId(String agentId);            // Mono
    Mono<Boolean> existsByAgentId(String agentId);        // Mono<Boolean>
    Flux<Agent> findByStatus(AgentStatus status);         // Flux

    @Query("SELECT * FROM agent WHERE last_heartbeat < :threshold")     // 네이티브 SQL
    Flux<Agent> findOfflineAgents(LocalDateTime threshold);

    @Query("SELECT COUNT(*) FROM agent WHERE status = :status")         // 네이티브 SQL COUNT
    Mono<Long> countByStatus(String status);
}
```

---

## Service 변환 - 핵심 패턴

### 반환 타입 변화

| 기존 | 리액티브 |
|------|---------|
| `T method()` | `Mono<T> method()` |
| `List<T> method()` | `Flux<T> method()` |
| `void method()` | `Mono<Void> method()` |
| `long count()` | `Mono<Long> count()` |

### 패턴 1: 단건 조회 + 예외 처리

```java
// 기존 (블로킹)
public AgentResponseDto getAgent(Long id) {
    Agent agent = agentRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("찾을 수 없습니다. id: " + id));
    return toResponseDto(agent);
}

// 리액티브 (논블로킹)
public Mono<AgentResponseDto> getAgent(Long id) {
    return agentRepository.findById(id)
            .switchIfEmpty(Mono.error(new IllegalArgumentException("찾을 수 없습니다. id: " + id)))
            .map(this::toResponseDto);
}
```

### 패턴 2: 존재 여부 확인 후 저장

```java
// 기존 (블로킹)
public AgentRegisterResponseDto registerAgent(AgentRegisterDto dto) {
    if (agentRepository.existsByAgentId(dto.getAgentId())) {   // 블로킹 체크
        throw new IllegalArgumentException("이미 등록된 ID입니다.");
    }
    Agent agent = Agent.builder()...build();
    return toResponseDto(agentRepository.save(agent));          // 블로킹 저장
}

// 리액티브 (논블로킹)
public Mono<AgentRegisterResponseDto> registerAgent(AgentRegisterDto dto) {
    return agentRepository.existsByAgentId(dto.getAgentId())   // Mono<Boolean>
            .flatMap(exists -> {
                if (exists) return Mono.error(new IllegalArgumentException("이미 등록된 ID입니다."));
                Agent agent = Agent.builder()...build();
                return agentRepository.save(agent);             // Mono<Agent>
            })
            .map(this::toResponseDto);                          // Mono<ResponseDto>
}
```

### 패턴 3: 여러 작업 순서 체이닝

```java
// 기존 (블로킹) - 순서 보장이 암묵적
public AgentHeartbeat saveHeartbeat(Long agentId, ...) {
    Agent agent = agentRepository.findById(agentId).orElseThrow(...);
    AgentHeartbeat heartbeat = heartbeatRepository.save(...);   // 1번 저장
    agent.setLastHeartbeat(heartbeat.getHeartbeatAt());
    agentRepository.save(agent);                                // 2번 저장
    return heartbeat;
}

// 리액티브 (논블로킹) - flatMap으로 순서 명시
public Mono<AgentHeartbeat> saveHeartbeat(Long agentId, ...) {
    return agentRepository.findById(agentId)
            .switchIfEmpty(Mono.error(...))
            .flatMap(agent ->
                heartbeatRepository.save(heartbeat)             // 1번 저장
                        .flatMap(saved -> {
                            agent.setLastHeartbeat(saved.getHeartbeatAt());
                            return agentRepository.save(agent)  // 2번 저장
                                    .thenReturn(saved);         // 원래 결과 반환
                        })
            );
}
```

### 패턴 4: 목록 변환

```java
// 기존 (블로킹)
public List<AgentResponseDto> getAllAgents() {
    return agentRepository.findAll()
            .stream()
            .map(this::toResponseDto)
            .collect(Collectors.toList());
}

// 리액티브 (논블로킹) - Flux는 스트림 연산 내장
public Flux<AgentResponseDto> getAllAgents() {
    return agentRepository.findAll()
            .map(this::toResponseDto);    // Flux.map = stream().map()
}
```

### 패턴 5: void → Mono<Void>

```java
// 기존 (블로킹)
public void deleteTask(Long id) {
    Task task = taskRepository.findById(id).orElseThrow(...);
    taskRepository.delete(task);
}

// 리액티브 (논블로킹)
public Mono<Void> deleteTask(Long id) {
    return taskRepository.findById(id)
            .switchIfEmpty(Mono.error(...))
            .flatMap(task -> taskRepository.delete(task));   // Mono<Void>
}
```

---

## Security 변환

### 클래스 대응표

| 기존 (MVC) | 리액티브 (WebFlux) |
|-----------|------------------|
| `@EnableWebSecurity` | `@EnableWebFluxSecurity` |
| `HttpSecurity` | `ServerHttpSecurity` |
| `SecurityFilterChain` | `SecurityWebFilterChain` |
| `.authorizeHttpRequests()` | `.authorizeExchange()` |
| `.requestMatchers(...)` | `.pathMatchers(...)` |
| `.anyRequest()` | `.anyExchange()` |
| `AuthenticationManager` | `ReactiveAuthenticationManager` |
| `UserDetailsService` | `ReactiveUserDetailsService` |
| `OncePerRequestFilter` | `WebFilter` |
| `.addFilterBefore(filter, Class.class)` | `.addFilterBefore(filter, SecurityWebFiltersOrder.X)` |

### SecurityConfig 비교

```java
// 기존 (MVC)
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(apiKeyFilter, UsernamePasswordAuthenticationFilter.class)
            .formLogin(form -> form
                .defaultSuccessUrl("/dashboard", true)
                .failureUrl("/login?error=true")
            );
        return http.build();
    }
}

// 리액티브 (WebFlux)
@EnableWebFluxSecurity
public class SecurityConfig {
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(csrf -> csrf.requireCsrfProtectionMatcher(
                new NegatedServerWebExchangeMatcher(
                    ServerWebExchangeMatchers.pathMatchers("/api/**"))))
            .authorizeExchange(auth -> auth
                .pathMatchers("/login").permitAll()
                .anyExchange().authenticated()
            )
            .addFilterBefore(apiKeyFilter, SecurityWebFiltersOrder.AUTHENTICATION)
            .formLogin(form -> form
                .authenticationSuccessHandler(new RedirectServerAuthenticationSuccessHandler("/dashboard"))
                .authenticationFailureHandler(new RedirectServerAuthenticationFailureHandler("/login?error=true"))
            )
            .build();
    }
}
```

### ApiKeyFilter 비교

```java
// 기존 (MVC) - OncePerRequestFilter
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String apiKey = request.getHeader("Authorization");

        Agent agent = agentService.findByApiKey(apiKey);      // 블로킹 조회

        // SecurityContext에 직접 설정
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);              // 다음 필터 (블로킹)
    }
}

// 리액티브 (WebFlux) - WebFilter
public class ApiKeyAuthenticationFilter implements WebFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String apiKey = exchange.getRequest().getHeaders().getFirst("Authorization");

        return agentService.findByApiKey(apiKey)              // 논블로킹 조회 (Mono)
                .flatMap(agent ->
                    chain.filter(exchange)                    // 다음 필터 (Mono)
                            .contextWrite(ReactiveSecurityContextHolder
                                    .withAuthentication(authentication))  // Context로 주입
                )
                .onErrorResume(e -> {
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().writeWith(...);
                });
    }
}
```

### UserDetailsService 비교

```java
// 기존 (MVC)
public class CustomUserDetailsService implements UserDetailsService {
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("..."));  // 블로킹
        return User.builder()...build();
    }
}

// 리액티브 (WebFlux)
public class CustomUserDetailsService implements ReactiveUserDetailsService {
    @Override
    public Mono<UserDetails> findByUsername(String email) {
        return memberRepository.findByEmail(email)             // Mono<Member>
                .switchIfEmpty(Mono.error(new UsernameNotFoundException("...")))
                .flatMap(member ->
                    Mono.just(User.builder()...build())        // Mono<UserDetails>
                );
    }
}
```

---

## 실시간 통신: WebSocket → SSE

### 구조 비교

| 항목 | 기존 (WebSocket/STOMP) | 리액티브 (SSE) |
|------|----------------------|--------------|
| 서버 설정 | `@EnableWebSocketMessageBroker` | 없음 (일반 REST 엔드포인트) |
| 서버 발행 | `SimpMessagingTemplate.convertAndSend()` | `Sinks.Many.tryEmitNext()` |
| 엔드포인트 | `/ws` (SockJS) | `/api/sse/events` |
| 응답 타입 | 양방향 (WebSocket) | 단방향 서버→클라이언트 |
| 클라이언트 라이브러리 | SockJS + STOMP.js (CDN) | `EventSource` (JS 표준 내장) |
| 재연결 | 수동 (`setTimeout`) | 자동 (브라우저 내장) |

### 서버 코드 비교

```java
// 기존 (WebSocket)
@Service
public class WebSocketService {
    private final SimpMessagingTemplate messagingTemplate;

    public void broadcastMetric(Long agentId, Object data) {
        WebSocketMessageDto message = ...;
        messagingTemplate.convertAndSend("/topic/metrics", message);  // STOMP 토픽으로 발행
    }
}

// 리액티브 (SSE)
@Service
public class SseService {
    // Sinks.Many: 여러 구독자에게 멀티캐스트하는 Hot Publisher
    private final Sinks.Many<WebSocketMessageDto> sink =
            Sinks.many().multicast().onBackpressureBuffer();

    public void broadcastMetric(Long agentId, Object data) {
        WebSocketMessageDto message = ...;
        sink.tryEmitNext(message);  // 연결된 모든 클라이언트에게 발행
    }

    public Flux<WebSocketMessageDto> getStream() {
        return sink.asFlux();  // 클라이언트 연결 시 이 Flux를 구독
    }
}

@RestController
public class SseController {
    @GetMapping(value = "/api/sse/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<WebSocketMessageDto>> streamEvents() {
        return sseService.getStream()
                .map(msg -> ServerSentEvent.<WebSocketMessageDto>builder()
                        .event(msg.getType())  // METRIC, EXCEPTION, HEARTBEAT, AGENT_STATUS
                        .data(msg)
                        .build());
    }
}
```

### 클라이언트 코드 비교 (dashboard.html)

```javascript
// 기존 (SockJS + STOMP)
const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);
stompClient.connect({}, function(frame) {
    stompClient.subscribe('/topic/metrics', function(message) {
        const data = JSON.parse(message.body);
        handleMetricUpdate(data);
    });
});
// 페이지 종료 시
stompClient.disconnect();

// 리액티브 (EventSource - JS 표준 API)
const eventSource = new EventSource('/api/sse/events');
eventSource.addEventListener('METRIC', function(event) {
    const data = JSON.parse(event.data);  // event.body → event.data
    handleMetricUpdate(data);
});
// 자동 재연결 (브라우저 내장)
// 페이지 종료 시
eventSource.close();
```

---

## REST API Controller 변환

### 반환 타입 변화

| 기존 | 리액티브 |
|------|---------|
| `ResponseEntity<List<T>>` | `Flux<T>` (WebFlux가 JSON 배열로 직렬화) |
| `ResponseEntity<T>` | `Mono<ResponseEntity<T>>` |
| `ResponseEntity<Void>` | `Mono<ResponseEntity<Void>>` |
| try-catch + return | `.onErrorReturn(ResponseEntity.badRequest().build())` |

### 코드 비교

```java
// 기존 (MVC)
@GetMapping
public ResponseEntity<List<AgentResponseDto>> getAllAgents() {
    List<AgentResponseDto> agents = agentService.getAllAgents();    // 블로킹
    return ResponseEntity.ok(agents);
}

@PostMapping("/register")
public ResponseEntity<AgentRegisterResponseDto> registerAgent(@RequestBody AgentRegisterDto dto) {
    try {
        AgentRegisterResponseDto response = agentService.registerAgent(dto);   // 블로킹
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest().build();
    }
}

// 리액티브 (WebFlux)
@GetMapping
public Flux<AgentResponseDto> getAllAgents() {
    return agentService.getAllAgents();    // Flux 그대로 반환
}

@PostMapping("/register")
public Mono<ResponseEntity<AgentRegisterResponseDto>> registerAgent(@RequestBody AgentRegisterDto dto) {
    return agentService.registerAgent(dto)
            .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response))
            .onErrorReturn(IllegalArgumentException.class,
                    ResponseEntity.badRequest().build());
}
```

---

## Thymeleaf Controller 변환

### 주요 변경사항

| 기존 (MVC) | 리액티브 (WebFlux) |
|-----------|------------------|
| `String` 반환 | `Mono<String>` (DB 조회 있는 경우) |
| `Page<T>` + `Pageable` | `Flux<T>` (페이징 제거) |
| `RedirectAttributes` | URL 파라미터 (`?success=true`) |
| `model.addAttribute("list", List)` | `model.addAttribute("list", Flux)` |
| 동기 redirect 분기 | `Mono<Boolean>` 체이닝으로 분기 |

### 코드 비교

```java
// 기존 (MVC)
@GetMapping("/members/me")
public String myProfile(Authentication auth) {
    Member member = memberService.findByEmail(auth.getName());  // 블로킹
    return "redirect:/members/" + member.getId();
}

@GetMapping("/members/{id}")
public String detail(@PathVariable Long id, Model model, Authentication auth) {
    if (!canAccessMember(id, auth)) return "redirect:/dashboard";   // 동기 체크
    MemberDetailDto dto = memberService.getMemberDetail(id);        // 블로킹
    model.addAttribute("member", dto);
    return "members/detail";
}

// 리액티브 (WebFlux)
@GetMapping("/members/me")
public Mono<String> myProfile(Authentication auth) {
    return memberService.findByEmail(auth.getName())   // Mono<Member>
            .map(member -> "redirect:/members/" + member.getId())
            .onErrorReturn("redirect:/login");
}

@GetMapping("/members/{id}")
public Mono<String> detail(@PathVariable Long id, Model model, Authentication auth) {
    return canAccessMemberMono(id, auth)               // Mono<Boolean>
            .flatMap(canAccess -> {
                if (!canAccess) return Mono.just("redirect:/dashboard");
                return memberService.getMemberDetail(id)  // Mono<MemberDetailDto>
                        .doOnNext(dto -> model.addAttribute("member", dto))
                        .thenReturn("members/detail");
            });
}
```

> **Thymeleaf + WebFlux**: `model.addAttribute("list", flux)`처럼  
> Flux를 모델에 넣으면 Thymeleaf가 렌더링 시 자동으로 subscribe해서 처리한다.

---

## 설정 파일 변경

### application.properties

```properties
# 기존 (JPA)
spring.datasource.url=jdbc:mariadb://localhost:3306/ledmega
spring.datasource.username=ledmega
spring.datasource.password=

spring.jpa.hibernate.ddl-auto=none
spring.jpa.show-sql=true

# 리액티브 (R2DBC)
# R2DBC 연결 (논블로킹 드라이버)
spring.r2dbc.url=r2dbc:mariadb://localhost:3306/ledmega
spring.r2dbc.username=ledmega
spring.r2dbc.password=

# JDBC는 DatabaseInitializer(DriverManager 직접 사용)용으로만 유지
spring.datasource.url=jdbc:mariadb://localhost:3306/ledmega?createDatabaseIfNotExist=true
spring.datasource.username=ledmega
spring.datasource.password=

# Spring DataSource 빈 자동설정 제외 (JDBC 드라이버만 classpath에 있으면 됨)
spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
```

### R2dbcConfig (신설)

```java
// 기존: @EnableJpaAuditing (JPA 설정 클래스)
// 리액티브: @EnableR2dbcAuditing (@CreatedDate, @LastModifiedDate 활성화)
@Configuration
@EnableR2dbcAuditing
public class R2dbcConfig {
}
```

---

## Mono/Flux 핵심 연산자 정리

### 변환 연산자

| 연산자 | 설명 | 예시 |
|--------|------|------|
| `.map(f)` | 동기 변환 (1:1) | `.map(entity -> toDto(entity))` |
| `.flatMap(f)` | 비동기 변환 (1:Mono) | `.flatMap(id -> repo.findById(id))` |
| `.then()` | 결과 버리고 `Mono<Void>` 반환 | `repo.save(x).then()` |
| `.thenReturn(v)` | 결과 버리고 특정 값 반환 | `repo.save(x).thenReturn("ok")` |
| `.next()` | Flux → Mono (첫 번째 요소) | `flux.findAll().next()` |

### 에러 처리

| 연산자 | 설명 |
|--------|------|
| `Mono.error(ex)` | 에러 시그널 발생 |
| `.switchIfEmpty(Mono.error(...))` | empty 시 에러로 전환 (`orElseThrow` 대체) |
| `.onErrorReturn(value)` | 에러 시 기본값 반환 |
| `.onErrorReturn(ExClass, value)` | 특정 예외 타입일 때만 기본값 반환 |
| `.onErrorResume(f)` | 에러 시 다른 Publisher로 전환 |
| `.doOnNext(f)` | 값 흐름에 사이드이펙트 추가 (로그 등) |
| `.doOnSuccess(f)` | 완료 시 사이드이펙트 (void 성공 시) |

### Sinks (Hot Publisher)

```java
// Sinks.Many: 여러 구독자에게 멀티캐스트
Sinks.Many<T> sink = Sinks.many().multicast().onBackpressureBuffer();

// 발행 (Publisher 쪽)
sink.tryEmitNext(value);

// 구독 (Subscriber 쪽 - 매 연결마다 새 구독 생성)
Flux<T> stream = sink.asFlux();
```

---

## 핵심 개념 요약

```
블로킹 (기존)            논블로킹 (리액티브)
─────────────────────   ─────────────────────────────────
DB조회 → 스레드 대기     DB조회 → 즉시 반환(Mono) → 완료시 콜백
순차 실행               연산자 체이닝으로 파이프라인 구성
try-catch               Mono.error + onErrorReturn
if/else                 flatMap 내부 분기
List 반환               Flux 반환
void 반환               Mono<Void> 반환
```

---

*참고: `main` 브랜치 = 블로킹(JPA+MVC), `feature/reactive` 브랜치 = 논블로킹(R2DBC+WebFlux)*
