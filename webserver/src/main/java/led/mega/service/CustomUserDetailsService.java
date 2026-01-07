package led.mega.service;

import led.mega.entity.Member;
import led.mega.entity.MemberStatus;
import led.mega.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("로그인 실패: 이메일을 찾을 수 없습니다. email={}", email);
                    return new UsernameNotFoundException("이메일 또는 비밀번호가 올바르지 않습니다.");
                });

        // 비활성화된 회원 체크
        if (member.getStatus() != MemberStatus.ACTIVE) {
            log.warn("로그인 실패: 비활성화된 회원. email={}, status={}", email, member.getStatus());
            throw new UsernameNotFoundException("비활성화된 계정입니다.");
        }

        // 권한 설정
        List<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority(member.getRole().name())
        );

        log.info("로그인 시도: email={}, role={}", email, member.getRole());

        return User.builder()
                .username(member.getEmail())
                .password(member.getPassword())
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(member.getStatus() != MemberStatus.ACTIVE)
                .build();
    }
}

