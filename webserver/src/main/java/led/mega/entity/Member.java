package led.mega.entity;

// [REACTIVE] JPA → R2DBC 전환
// - jakarta.persistence.* → Spring Data R2DBC 어노테이션
// - @Enumerated 제거: R2DBC는 Enum을 name() 문자열로 자동 변환

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("member")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Member {

    @Id
    private Long id;

    private String email;
    private String password;
    private String name;
    private String nickname;
    private String phone;

    @Builder.Default
    private MemberRole role = MemberRole.ROLE_USER;

    @Builder.Default
    private MemberStatus status = MemberStatus.ACTIVE;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private LocalDateTime lastLoginAt;
}

