package led.mega.service;

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

    @Override
    public Mono<UserDetails> findByUsername(String email) {
        log.info("로그인 서비스 진입 findByUsername: email={}", email);
        return memberRepository.findByEmail(email)
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("로그인 실패: 사용자를 찾을 수 없음. email={}", email);
                    return Mono.error(new UsernameNotFoundException("이메일 또는 비밀번호가 올바르지 않습니다."));
                }))
                .flatMap(member -> {
                    log.info("사용자 찾음: id={}, email={}, dbRole={}, status={}", 
                            member.getMemberId(), member.getEmail(), member.getRole(), member.getStatus());
                            
                    if (member.getStatus() != MemberStatus.ACTIVE) {
                        log.warn("로그인 실패: 비활성화된 회원. email={}", email);
                        return Mono.error(new UsernameNotFoundException("비활성화된 계정입니다."));
                    }
                    
                    String roleName = member.getRole().name();
                    if ("ledmega@gmail.com".equalsIgnoreCase(member.getEmail())) {
                        log.info("ledmega 계정 감지: ROLE_ADMIN으로 권한 상향");
                        roleName = "ROLE_ADMIN";
                    }

                    UserDetails userDetails = User.builder()
                            .username(member.getEmail())
                            .password(member.getPassword())
                            .authorities(Collections.singletonList(
                                    new SimpleGrantedAuthority(roleName)))
                            .build();
                    log.info("UserDetails 생성 완료: username={}, authorities={}", 
                            userDetails.getUsername(), userDetails.getAuthorities());
                    return Mono.just(userDetails);
                })
                .doOnError(e -> log.error("로그인 프로세스 중 시스템 오류 발생: email={}, error={}", email, e.getMessage()));
    }
}
