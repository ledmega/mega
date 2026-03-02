package led.mega.service;

import led.mega.dto.MemberDetailDto;
import led.mega.dto.MemberUpdateDto;
import led.mega.dto.SignupDto;
import led.mega.entity.Member;
import led.mega.entity.MemberRole;
import led.mega.entity.MemberStatus;
import led.mega.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.Map;

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

    /**
     * ID로 회원 조회
     */
    public Member findById(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다. id: " + id));
    }

    /**
     * 회원 목록 (페이징, 검색: 이메일/이름)
     */
    public Page<MemberDetailDto> getMemberPage(String search, Pageable pageable) {
        if (search != null && !search.isBlank()) {
            String term = search.trim();
            return memberRepository.findByEmailContainingOrNameContaining(term, term, pageable)
                    .map(MemberDetailDto::from);
        }
        return memberRepository.findAll(pageable).map(MemberDetailDto::from);
    }

    /**
     * 회원 상세 DTO
     */
    public MemberDetailDto getMemberDetail(Long id) {
        return MemberDetailDto.from(findById(id));
    }

    /**
     * 회원 정보 수정 (이름, 닉네임, 전화번호; 관리자는 역할/상태 포함)
     */
    @Transactional
    public MemberDetailDto updateMember(Long id, MemberUpdateDto dto, boolean isAdmin) {
        Member member = findById(id);
        if (dto.getName() != null) {
            member.setName(dto.getName());
        }
        if (dto.getNickname() != null) {
            member.setNickname(dto.getNickname());
        }
        if (dto.getPhone() != null) {
            member.setPhone(dto.getPhone());
        }
        if (isAdmin) {
            if (dto.getRole() != null) {
                member.setRole(dto.getRole());
            }
            if (dto.getStatus() != null) {
                member.setStatus(dto.getStatus());
            }
        }
        memberRepository.save(member);
        log.info("회원 정보 수정: id={}, email={}", member.getId(), member.getEmail());
        return MemberDetailDto.from(member);
    }

    /**
     * 회원 상태 변경 (관리자 전용)
     */
    @Transactional
    public MemberDetailDto updateStatus(Long id, MemberStatus status) {
        Member member = findById(id);
        member.setStatus(status);
        memberRepository.save(member);
        log.info("회원 상태 변경: id={}, status={}", id, status);
        return MemberDetailDto.from(member);
    }

    /**
     * 역할별 회원 수 (권한관리용)
     */
    public Map<MemberRole, Long> getMemberCountByRole() {
        Map<MemberRole, Long> map = new EnumMap<>(MemberRole.class);
        for (MemberRole role : MemberRole.values()) {
            map.put(role, memberRepository.countByRole(role));
        }
        return map;
    }

    /**
     * 역할별 회원 목록 (페이징)
     */
    public Page<MemberDetailDto> getMembersByRole(MemberRole role, Pageable pageable) {
        return memberRepository.findByRoleOrderByCreatedAtDesc(role, pageable)
                .map(MemberDetailDto::from);
    }

    /**
     * 회원 역할 변경 (관리자 전용)
     */
    @Transactional
    public MemberDetailDto updateRole(Long id, MemberRole role) {
        Member member = findById(id);
        member.setRole(role);
        memberRepository.save(member);
        log.info("회원 역할 변경: id={}, role={}", id, role);
        return MemberDetailDto.from(member);
    }
}


