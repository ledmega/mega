package led.mega.controller;

// [REACTIVE] AuthorityController 전환
// - Pageable 제거 → Flux (전체 목록)
// - RedirectAttributes 제거 → URL 파라미터
// - Map<MemberRole,Long> → Mono<Map<MemberRole,Long>>
// - 반환타입: Mono<String>

import led.mega.entity.MemberRole;
import led.mega.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Controller
@RequestMapping("/authority")
@RequiredArgsConstructor
public class AuthorityController {

    private final MemberService memberService;

    // [CHANGED] Mono<String>으로 전환, Pageable 제거, countByRole → Mono<Map>
    @GetMapping
    public Mono<String> list(@RequestParam(required = false) String roleFilter,
                             Model model,
                             Authentication auth) {
        if (!isAdmin(auth)) return Mono.just("redirect:/dashboard");

        MemberRole filter = parseRole(roleFilter);

        return memberService.getMemberCountByRole()
                .doOnNext(countByRole -> {
                    model.addAttribute("countByRole", countByRole);
                    model.addAttribute("allRoles", MemberRole.values());
                    model.addAttribute("roleFilter", filter != null ? filter.name() : null);
                    if (filter != null) {
                        model.addAttribute("memberPage", memberService.getMembersByRole(filter)); // Flux
                    } else {
                        model.addAttribute("memberPage", memberService.getMemberPage(null)); // Flux
                    }
                })
                .thenReturn("authority/list");
    }

    // [CHANGED] Mono<String>, RedirectAttributes 제거 → URL 파라미터
    @PostMapping("/members/{id}/role")
    public Mono<String> updateRole(@PathVariable Long id,
                                   @RequestParam MemberRole role,
                                   Authentication auth) {
        if (!isAdmin(auth)) return Mono.just("redirect:/dashboard");
        return memberService.updateRole(id, role)
                .thenReturn("redirect:/authority?roleFilter=" + role.name())
                .onErrorReturn("redirect:/authority?error=true");
    }

    private boolean isAdmin(Authentication auth) {
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_ADMIN"));
    }

    private MemberRole parseRole(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return MemberRole.valueOf(value.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
