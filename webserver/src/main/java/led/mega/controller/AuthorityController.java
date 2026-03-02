package led.mega.controller;

import led.mega.entity.MemberRole;
import led.mega.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

/**
 * 권한(역할) 관리 - 관리자 전용.
 */
@Controller
@RequestMapping("/authority")
@RequiredArgsConstructor
public class AuthorityController {

    private final MemberService memberService;

    /** 권한관리 메인: 역할별 통계 + 역할별 회원 목록 */
    @GetMapping
    public String list(@RequestParam(required = false) String roleFilter,
                       @PageableDefault(size = 20, sort = "createdAt") Pageable pageable,
                       Model model,
                       Authentication auth) {
        if (!isAdmin(auth)) {
            return "redirect:/dashboard";
        }
        Map<MemberRole, Long> countByRole = memberService.getMemberCountByRole();
        model.addAttribute("countByRole", countByRole);

        MemberRole filter = parseRole(roleFilter);
        if (filter != null) {
            Page<?> memberPage = memberService.getMembersByRole(filter, pageable);
            model.addAttribute("memberPage", memberPage);
            model.addAttribute("roleFilter", filter.name());
        } else {
            Page<?> memberPage = memberService.getMemberPage(null, pageable);
            model.addAttribute("memberPage", memberPage);
            model.addAttribute("roleFilter", null);
        }
        model.addAttribute("allRoles", MemberRole.values());
        return "authority/list";
    }

    /** 회원 역할 변경 */
    @PostMapping("/members/{id}/role")
    public String updateRole(@PathVariable Long id,
                             @RequestParam MemberRole role,
                             Authentication auth,
                             RedirectAttributes redirectAttributes) {
        if (!isAdmin(auth)) {
            return "redirect:/dashboard";
        }
        try {
            memberService.updateRole(id, role);
            redirectAttributes.addFlashAttribute("successMessage", "역할이 변경되었습니다.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        redirectAttributes.addAttribute("roleFilter", role.name());
        return "redirect:/authority";
    }

    private boolean isAdmin(Authentication auth) {
        return auth != null && auth.getAuthorities() != null
                && auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private MemberRole parseRole(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return MemberRole.valueOf(value.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
