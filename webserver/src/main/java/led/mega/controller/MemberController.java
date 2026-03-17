package led.mega.controller;

import jakarta.validation.Valid;
import led.mega.dto.MemberUpdateDto;
import led.mega.dto.SignupDto;
import led.mega.entity.MemberStatus;
import led.mega.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@Controller
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/signup")
    public String signupForm(Model model) {
        model.addAttribute("signupDto", new SignupDto());
        return "signup";
    }

    @PostMapping("/signup")
    public Mono<String> signup(@Valid @ModelAttribute SignupDto signupDto,
                               BindingResult bindingResult,
                               Model model) {
        if (bindingResult.hasErrors()) {
            return Mono.just("signup");
        }
        return memberService.signup(signupDto)
                .thenReturn("redirect:/login?success=true")
                .onErrorResume(IllegalArgumentException.class, e -> {
                    bindingResult.rejectValue("email", "error.email", e.getMessage());
                    return Mono.just("signup");
                })
                .onErrorResume(e -> {
                    log.error("회원가입 중 오류 발생", e);
                    model.addAttribute("error", "회원가입 중 오류가 발생했습니다.");
                    return Mono.just("signup");
                });
    }

    @GetMapping("/members/me")
    public Mono<String> myProfile(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return Mono.just("redirect:/login");
        }
        return memberService.findByEmail(auth.getName())
                .map(member -> "redirect:/members/" + member.getMemberId())
                .onErrorReturn("redirect:/login");
    }

    @GetMapping("/members")
    public Mono<String> list(@RequestParam(required = false) String search,
                             Model model, Authentication auth) {
        if (!isAdmin(auth)) return Mono.just("redirect:/dashboard");
        model.addAttribute("memberPage", memberService.getMemberPage(search));
        model.addAttribute("search", search);
        return Mono.just("members/list");
    }

    @GetMapping("/members/{id}")
    public Mono<String> detail(@PathVariable String id, Model model, Authentication auth) {
        return canAccessMemberMono(id, auth)
                .flatMap(canAccess -> {
                    if (!canAccess) return Mono.just("redirect:/dashboard");
                    return memberService.getMemberDetail(id)
                            .doOnNext(dto -> {
                                model.addAttribute("member", dto);
                                model.addAttribute("isAdmin", isAdmin(auth));
                            })
                            .thenReturn("members/detail");
                })
                .onErrorReturn("redirect:/dashboard");
    }

    @GetMapping("/members/{id}/edit")
    public Mono<String> editForm(@PathVariable String id, Model model, Authentication auth) {
        return canAccessMemberMono(id, auth)
                .flatMap(canAccess -> {
                    if (!canAccess) return Mono.just("redirect:/dashboard");
                    return memberService.getMemberDetail(id)
                            .doOnNext(dto -> {
                                MemberUpdateDto updateDto = new MemberUpdateDto();
                                updateDto.setName(dto.getName());
                                updateDto.setNickname(dto.getNickname());
                                updateDto.setPhone(dto.getPhone());
                                updateDto.setRole(dto.getRole());
                                updateDto.setStatus(dto.getStatus());
                                model.addAttribute("member", dto);
                                model.addAttribute("memberUpdateDto", updateDto);
                                model.addAttribute("isAdmin", isAdmin(auth));
                            })
                            .thenReturn("members/edit");
                })
                .onErrorReturn("redirect:/dashboard");
    }

    @PostMapping("/members/{id}/edit")
    public Mono<String> edit(@PathVariable String id,
                             @Valid @ModelAttribute MemberUpdateDto memberUpdateDto,
                             BindingResult bindingResult,
                             Model model,
                             Authentication auth) {
        return canAccessMemberMono(id, auth)
                .flatMap(canAccess -> {
                    if (!canAccess) return Mono.just("redirect:/dashboard");
                    if (bindingResult.hasErrors()) {
                        return memberService.getMemberDetail(id)
                                .doOnNext(dto -> {
                                    model.addAttribute("member", dto);
                                    model.addAttribute("isAdmin", isAdmin(auth));
                                })
                                .thenReturn("members/edit");
                    }
                    return memberService.updateMember(id, memberUpdateDto, isAdmin(auth))
                            .thenReturn("redirect:/members/" + id)
                            .onErrorResume(IllegalArgumentException.class, e -> {
                                bindingResult.reject("error.member", e.getMessage());
                                return memberService.getMemberDetail(id)
                                        .doOnNext(dto -> {
                                            model.addAttribute("member", dto);
                                            model.addAttribute("isAdmin", isAdmin(auth));
                                        })
                                        .thenReturn("members/edit");
                            });
                });
    }

    @PostMapping("/members/{id}/status")
    public Mono<String> updateStatus(@PathVariable String id,
                                     @RequestParam MemberStatus status,
                                     Authentication auth) {
        if (!isAdmin(auth)) return Mono.just("redirect:/dashboard");
        return memberService.updateStatus(id, status)
                .thenReturn("redirect:/members/" + id)
                .onErrorReturn("redirect:/members/" + id + "?error=true");
    }

    private boolean isAdmin(Authentication auth) {
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_ADMIN"));
    }

    private Mono<Boolean> canAccessMemberMono(String memberId, Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return Mono.just(false);
        if (isAdmin(auth)) return Mono.just(true);
        return memberService.findByEmail(auth.getName())
                .map(current -> current.getMemberId().equals(memberId))
                .onErrorReturn(false);
    }
}
