package led.mega.controller;

import led.mega.entity.MemberRole;
import led.mega.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Controller
@RequestMapping("/authority")
@RequiredArgsConstructor
public class AuthorityController {

    private final MemberService memberService;

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
                        model.addAttribute("memberPage", memberService.getMembersByRole(filter));
                    } else {
                        model.addAttribute("memberPage", memberService.getMemberPage(null));
                    }
                })
                .thenReturn("authority/list");
    }

    @PostMapping("/members/{id}/role")
    public Mono<String> updateRole(@PathVariable String id,
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
