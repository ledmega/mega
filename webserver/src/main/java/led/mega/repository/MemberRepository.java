package led.mega.repository;

import led.mega.entity.Member;
import led.mega.entity.MemberRole;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface MemberRepository extends ReactiveCrudRepository<Member, String> {

    Mono<Member> findByEmail(String email);

    Mono<Boolean> existsByEmail(String email);

    Flux<Member> findByEmailContainingOrNameContaining(String email, String name);

    Mono<Long> countByRole(MemberRole role);

    @Query("SELECT * FROM member WHERE role = :role ORDER BY created_at DESC")
    Flux<Member> findByRoleOrderByCreatedAtDesc(String role);
}
