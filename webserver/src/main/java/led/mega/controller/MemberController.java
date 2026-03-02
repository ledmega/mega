package led.mega.controller;

import jakarta.validation.Valid;
import led.mega.dto.MemberDetailDto;
import led.mega.dto.MemberUpdateDto;
import led.mega.dto.SignupDto;
import led.mega.entity.Member;
import led.mega.entity.MemberStatus;
import led.mega.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
    public String signup(@Valid @ModelAttribute SignupDto signupDto,
                        BindingResult bindingResult,
                        RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return "signup";
        }

        try {
            memberService.signup(signupDto);
            redirectAttributes.addFlashAttribute("successMessage", "회원가입이 완료되었습니다. 로그인해주세요.");
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            bindingResult.rejectValue("email", "error.email", e.getMessage());
            return "signup";
        } catch (Exception e) {
            log.error("회원가입 중 오류 발생", e);
            bindingResult.reject("error.signup", "회원가입 중 오류가 발생했습니다. 다시 시도해주세요.");
            return "signup";
        }
    }

    // ---------- 회원관리 (로그인 필요) ----------

    /** 내 정보로 이동 (현재 로그인 회원 상세) */
    @GetMapping("/members/me")
    public String myProfile(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return "redirect:/login";
        }
        Member member = memberService.findByEmail(auth.getName());
        return "redirect:/members/" + member.getId();
    }

    /** 회원 목록 (관리자만) */
    @GetMapping("/members")
    public String list(@RequestParam(required = false) String search,
                       @PageableDefault(size = 20, sort = "id") Pageable pageable,
                       Model model,
                       Authentication auth) {
        if (!isAdmin(auth)) {
            return "redirect:/dashboard";
        }
        Page<MemberDetailDto> page = memberService.getMemberPage(search, pageable);
        model.addAttribute("memberPage", page);
        model.addAttribute("search", search);
        return "members/list";
    }

    /** 회원 상세 (관리자 또는 본인) */
    @GetMapping("/members/{id}")
    public String detail(@PathVariable Long id, Model model, Authentication auth) {
        if (!canAccessMember(id, auth)) {
            return "redirect:/dashboard";
        }
        MemberDetailDto dto = memberService.getMemberDetail(id);
        model.addAttribute("member", dto);
        model.addAttribute("isAdmin", isAdmin(auth));
        return "members/detail";
    }

    /** 회원 수정 폼 (관리자 또는 본인) */
    @GetMapping("/members/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, Authentication auth) {
        if (!canAccessMember(id, auth)) {
            return "redirect:/dashboard";
        }
        MemberDetailDto dto = memberService.getMemberDetail(id);
        MemberUpdateDto updateDto = new MemberUpdateDto();
        updateDto.setName(dto.getName());
        updateDto.setNickname(dto.getNickname());
        updateDto.setPhone(dto.getPhone());
        updateDto.setRole(dto.getRole());
        updateDto.setStatus(dto.getStatus());
        model.addAttribute("member", dto);
        model.addAttribute("memberUpdateDto", updateDto);
        model.addAttribute("isAdmin", isAdmin(auth));
        return "members/edit";
    }

    /** 회원 수정 처리 */
    @PostMapping("/members/{id}/edit")
    public String edit(@PathVariable Long id,
                       @Valid @ModelAttribute MemberUpdateDto memberUpdateDto,
                       BindingResult bindingResult,
                       Model model,
                       Authentication auth,
                       RedirectAttributes redirectAttributes) {
        if (!canAccessMember(id, auth)) {
            return "redirect:/dashboard";
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("member", memberService.getMemberDetail(id));
            model.addAttribute("isAdmin", isAdmin(auth));
            return "members/edit";
        }
        try {
            boolean admin = isAdmin(auth);
            memberService.updateMember(id, memberUpdateDto, admin);
            redirectAttributes.addFlashAttribute("successMessage", "회원 정보가 수정되었습니다.");
            return "redirect:/members/" + id;
        } catch (IllegalArgumentException e) {
            bindingResult.reject("error.member", e.getMessage());
            model.addAttribute("member", memberService.getMemberDetail(id));
            model.addAttribute("isAdmin", isAdmin(auth));
            return "members/edit";
        }
    }

    /** 회원 상태 변경 (관리자만) */
    @PostMapping("/members/{id}/status")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam MemberStatus status,
                               Authentication auth,
                               RedirectAttributes redirectAttributes) {
        if (!isAdmin(auth)) {
            return "redirect:/dashboard";
        }
        try {
            memberService.updateStatus(id, status);
            redirectAttributes.addFlashAttribute("successMessage", "회원 상태가 변경되었습니다.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/members/" + id;
    }

    private boolean isAdmin(Authentication auth) {
        return auth != null && auth.getAuthorities() != null
                && auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private boolean canAccessMember(Long memberId, Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        if (isAdmin(auth)) {
            return true;
        }
        try {
            Member current = memberService.findByEmail(auth.getName());
            return current.getId().equals(memberId);
        } catch (Exception e) {
            return false;
        }
    }
}


