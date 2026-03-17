package led.mega.service;

import led.mega.dto.MemberDetailDto;
import led.mega.dto.MemberUpdateDto;
import led.mega.dto.SignupDto;
import led.mega.entity.Member;
import led.mega.entity.MemberRole;
import led.mega.entity.MemberStatus;
import led.mega.repository.MemberRepository;
import led.mega.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.EnumMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Mono<Member> signup(SignupDto signupDto) {
        return memberRepository.existsByEmail(signupDto.getEmail())
                .flatMap(exists -> {
                    if (exists) return Mono.error(new IllegalArgumentException("이미 사용 중인 이메일입니다."));
                    Member member = Member.builder()
                            .memberId(IdGenerator.generate(IdGenerator.MEMBER))
                            .email(signupDto.getEmail())
                            .password(passwordEncoder.encode(signupDto.getPassword()))
                            .name(signupDto.getName())
                            .nickname(signupDto.getNickname())
                            .phone(signupDto.getPhone())
                            .role(MemberRole.ROLE_USER)
                            .status(MemberStatus.ACTIVE)
                            .build();
                    return memberRepository.save(member);
                })
                .doOnNext(m -> log.info("회원가입 완료: {}", m.getEmail()));
    }

    public Mono<Member> findByEmail(String email) {
        return memberRepository.findByEmail(email)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("회원을 찾을 수 없습니다.")));
    }

    public Mono<Member> findById(String id) {
        return memberRepository.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("회원을 찾을 수 없습니다. id: " + id)));
    }

    public Flux<MemberDetailDto> getMemberPage(String search) {
        if (search != null && !search.isBlank()) {
            String term = search.trim();
            return memberRepository.findByEmailContainingOrNameContaining(term, term)
                    .map(MemberDetailDto::from);
        }
        return memberRepository.findAll().map(MemberDetailDto::from);
    }

    public Mono<MemberDetailDto> getMemberDetail(String id) {
        return findById(id).map(MemberDetailDto::from);
    }

    @Transactional
    public Mono<MemberDetailDto> updateMember(String id, MemberUpdateDto dto, boolean isAdmin) {
        return findById(id)
                .flatMap(member -> {
                    if (dto.getName() != null) member.setName(dto.getName());
                    if (dto.getNickname() != null) member.setNickname(dto.getNickname());
                    if (dto.getPhone() != null) member.setPhone(dto.getPhone());
                    if (isAdmin) {
                        if (dto.getRole() != null) member.setRole(dto.getRole());
                        if (dto.getStatus() != null) member.setStatus(dto.getStatus());
                    }
                    return memberRepository.save(member);
                })
                .map(MemberDetailDto::from)
                .doOnNext(m -> log.info("회원 정보 수정: id={}", m.getId()));
    }

    @Transactional
    public Mono<MemberDetailDto> updateStatus(String id, MemberStatus status) {
        return findById(id)
                .flatMap(member -> {
                    member.setStatus(status);
                    return memberRepository.save(member);
                })
                .map(MemberDetailDto::from)
                .doOnNext(m -> log.info("회원 상태 변경: id={}, status={}", m.getId(), status));
    }

    public Mono<Map<MemberRole, Long>> getMemberCountByRole() {
        return Flux.fromArray(MemberRole.values())
                .flatMap(role -> memberRepository.countByRole(role)
                        .map(count -> Map.entry(role, count)))
                .collectMap(Map.Entry::getKey, Map.Entry::getValue,
                        () -> new EnumMap<>(MemberRole.class));
    }

    public Flux<MemberDetailDto> getMembersByRole(MemberRole role) {
        return memberRepository.findByRoleOrderByCreatedAtDesc(role.name())
                .map(MemberDetailDto::from);
    }

    @Transactional
    public Mono<MemberDetailDto> updateRole(String id, MemberRole role) {
        return findById(id)
                .flatMap(member -> {
                    member.setRole(role);
                    return memberRepository.save(member);
                })
                .map(MemberDetailDto::from)
                .doOnNext(m -> log.info("회원 역할 변경: id={}, role={}", m.getId(), role));
    }
}
