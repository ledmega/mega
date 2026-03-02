package led.mega.dto;

import led.mega.entity.Member;
import led.mega.entity.MemberRole;
import led.mega.entity.MemberStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 회원 상세/목록 노출용 DTO (비밀번호 제외)
 */
@Data
@Builder
public class MemberDetailDto {
    private Long id;
    private String email;
    private String name;
    private String nickname;
    private String phone;
    private MemberRole role;
    private MemberStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLoginAt;

    public static MemberDetailDto from(Member member) {
        return MemberDetailDto.builder()
                .id(member.getId())
                .email(member.getEmail())
                .name(member.getName())
                .nickname(member.getNickname())
                .phone(member.getPhone())
                .role(member.getRole())
                .status(member.getStatus())
                .createdAt(member.getCreatedAt())
                .updatedAt(member.getUpdatedAt())
                .lastLoginAt(member.getLastLoginAt())
                .build();
    }
}
