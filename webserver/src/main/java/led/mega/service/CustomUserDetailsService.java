package led.mega.service;

// [REACTIVE] UserDetailsService → ReactiveUserDetailsService
// - loadUserByUsername(String) : UserDetails  →  findByUsername(String) : Mono<UserDetails>
// - orElseThrow → switchIfEmpty(Mono.error(...))
// - if 체크 → flatMap + Mono.error(...)
// - @Transactional(readOnly=true) 제거: R2DBC는 논블로킹이므로 트랜잭션 불필요

import led.mega.entity.MemberStatus;
import led.mega.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements ReactiveUserDetailsService {

    private final MemberRepository memberRepository;

    // [CHANGED] UserDetails → Mono<UserDetails>
    @Override
    public Mono<UserDetails> findByUsername(String email) {
        return memberRepository.findByEmail(email)
                .switchIfEmpty(Mono.error(new UsernameNotFoundException("이메일 또는 비밀번호가 올바르지 않습니다.")))
                .flatMap(member -> {
                    // [CHANGED] if throw → flatMap + Mono.error
                    if (member.getStatus() != MemberStatus.ACTIVE) {
                        log.warn("로그인 실패: 비활성화된 회원. email={}", email);
                        return Mono.error(new UsernameNotFoundException("비활성화된 계정입니다."));
                    }
                    log.info("로그인 시도: email={}, role={}", email, member.getRole());
                    UserDetails userDetails = User.builder()
                            .username(member.getEmail())
                            .password(member.getPassword())
                            .authorities(Collections.singletonList(
                                    new SimpleGrantedAuthority(member.getRole().name())))
                            .build();
                    return Mono.just(userDetails);
                });
    }
}

