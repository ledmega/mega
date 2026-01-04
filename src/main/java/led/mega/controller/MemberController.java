package led.mega.controller;

import jakarta.validation.Valid;
import led.mega.dto.SignupDto;
import led.mega.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
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
        
        // 유효성 검사 실패
        if (bindingResult.hasErrors()) {
            return "signup";
        }

        try {
            // 회원가입 처리
            memberService.signup(signupDto);
            redirectAttributes.addFlashAttribute("successMessage", "회원가입이 완료되었습니다. 로그인해주세요.");
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            // 중복 이메일 등 오류 처리
            bindingResult.rejectValue("email", "error.email", e.getMessage());
            return "signup";
        } catch (Exception e) {
            log.error("회원가입 중 오류 발생", e);
            bindingResult.reject("error.signup", "회원가입 중 오류가 발생했습니다. 다시 시도해주세요.");
            return "signup";
        }
    }
}


