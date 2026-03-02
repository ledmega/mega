package led.mega.repository;

import led.mega.entity.Member;
import led.mega.entity.MemberRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByEmail(String email);
    boolean existsByEmail(String email);

    /** 이메일 또는 이름으로 검색 (페이징) */
    Page<Member> findByEmailContainingOrNameContaining(String email, String name, Pageable pageable);

    /** 역할별 회원 수 */
    long countByRole(MemberRole role);

    /** 역할별 회원 목록 (가입일 내림차순) */
    Page<Member> findByRoleOrderByCreatedAtDesc(MemberRole role, Pageable pageable);
}


