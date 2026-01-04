package led.mega.service;

import led.mega.dto.SignupDto;
import led.mega.entity.Member;
import led.mega.entity.MemberRole;
import led.mega.entity.MemberStatus;
import led.mega.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 회원가입
     */
    @Transactional
    public Member signup(SignupDto signupDto) {
        // 이메일 중복 확인
        if (memberRepository.existsByEmail(signupDto.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(signupDto.getPassword());

        // 회원 생성
        Member member = Member.builder()
                .email(signupDto.getEmail())
                .password(encodedPassword)
                .name(signupDto.getName())
                .nickname(signupDto.getNickname())
                .phone(signupDto.getPhone())
                .role(MemberRole.ROLE_USER)
                .status(MemberStatus.ACTIVE)
                .build();

        Member savedMember = memberRepository.save(member);
        log.info("회원가입 완료: {}", savedMember.getEmail());
        
        return savedMember;
    }

    /**
     * 이메일로 회원 조회
     */
    public Member findByEmail(String email) {
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
    }
}


