package led.mega.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("member")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Member implements Persistable<String> {

    @Id
    @Column("member_id")
    private String memberId;

    @Transient
    @Builder.Default
    private boolean isNew = false;

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
    @Column("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private LocalDateTime updatedAt;

    @Column("last_login_at")
    private LocalDateTime lastLoginAt;

    @Override
    public String getId() {
        return memberId;
    }

    @Override
    public boolean isNew() {
        return isNew || memberId == null;
    }
}
