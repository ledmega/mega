package led.mega.dto;

import jakarta.validation.constraints.Size;
import led.mega.entity.MemberRole;
import led.mega.entity.MemberStatus;
import lombok.Data;

@Data
public class MemberUpdateDto {
    @Size(max = 50, message = "이름은 50자 이하여야 합니다")
    private String name;

    @Size(max = 50, message = "닉네임은 50자 이하여야 합니다")
    private String nickname;

    @Size(max = 20, message = "전화번호는 20자 이하여야 합니다")
    private String phone;

    /** 관리자만 변경 가능 */
    private MemberRole role;

    /** 관리자만 변경 가능 */
    private MemberStatus status;
}
