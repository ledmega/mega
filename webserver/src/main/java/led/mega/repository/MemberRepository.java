package led.mega.repository;

// [REACTIVE] JpaRepository → ReactiveCrudRepository
// - Optional<Member>  → Mono<Member>
// - boolean           → Mono<Boolean>
// - long              → Mono<Long>
// - Page<Member>      → Flux<Member>  (R2DBC는 Page 미지원 → Flux로 전환)
// - Pageable 파라미터 제거 (무한 스크롤/오프셋 방식으로 처리 가능하나 학습용 단순화)

import led.mega.entity.Member;
import led.mega.entity.MemberRole;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface MemberRepository extends ReactiveCrudRepository<Member, Long> {

    // [CHANGED] Optional<Member> → Mono<Member>
    Mono<Member> findByEmail(String email);

    // [CHANGED] boolean → Mono<Boolean>
    Mono<Boolean> existsByEmail(String email);

    // [CHANGED] Page<Member> → Flux<Member>, Pageable 제거
    Flux<Member> findByEmailContainingOrNameContaining(String email, String name);

    // [CHANGED] long → Mono<Long>
    Mono<Long> countByRole(MemberRole role);

    // [CHANGED] Page<Member> → Flux<Member>, Pageable 제거, JPQL→네이티브 SQL
    @Query("SELECT * FROM member WHERE role = :role ORDER BY created_at DESC")
    Flux<Member> findByRoleOrderByCreatedAtDesc(String role);
}


