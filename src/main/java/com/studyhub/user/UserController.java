package com.studyhub.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;
    private final PasswordResetService passwordResetService;

    /**
     * 회원가입 폼
     */
    @GetMapping("/signup")
    public String showSignupForm(Model model) {
        model.addAttribute("form", new UserRegisterForm());
        return "user/signup";
    }

    /**
     * 회원가입 처리
     */
    @PostMapping("/signup")
    public String handleSignup(@ModelAttribute("form") UserRegisterForm form,
            Model model) {

        // 1) 간단 검증
        if (isBlank(form.getEmail()) || isBlank(form.getPassword())
                || isBlank(form.getConfirmPassword()) || isBlank(form.getName())) {
            model.addAttribute("errorMessage", "이메일, 이름, 비밀번호는 필수입니다.");
            return "user/signup";
        }

        if (!form.getPassword().equals(form.getConfirmPassword())) {
            model.addAttribute("errorMessage", "비밀번호와 비밀번호 확인이 일치하지 않습니다.");
            return "user/signup";
        }

        // 2) 이메일 중복 체크
        if (userRepository.findByEmail(form.getEmail()).isPresent()) {
            model.addAttribute("errorMessage", "이미 사용 중인 이메일입니다.");
            return "user/signup";
        }

        // 3) 사용자 생성
        User user = new User();
        user.setEmail(form.getEmail());
        user.setPassword(passwordEncoder.encode(form.getPassword()));
        user.setName(form.getName());
        user.setNickname(form.getNickname());
        user.setPhone(form.getPhone());
        user.setIntro(form.getIntro());
        user.setRole(UserRole.USER);
        // 이메일 인증 전이므로 false 유지 (User 엔티티 기본값 가정)
        user.setEmailVerified(false);

        userRepository.save(user);

        // 4) 이메일 인증 토큰 생성 + "발송"
        emailVerificationService.createAndSendVerification(user);

        // 가입 완료 안내 페이지로 이동
        model.addAttribute("email", user.getEmail());
        return "user/signup_done";
    }

    /**
     * 로그인 폼 화면 (실제 인증은 Spring Security가 처리)
     */
    @GetMapping("/login")
    public String showLoginForm() {
        return "user/login";
    }

    /**
     * 비밀번호 재설정 요청 폼
     */
    @GetMapping("/forgot-password")
    public String showForgotPasswordForm() {
        return "user/forgot_password";
    }

    /**
     * 비밀번호 재설정 요청 처리
     */
    @PostMapping("/forgot-password")
    public String handleForgotPassword(@RequestParam("email") String email,
            Model model) {

        if (isBlank(email)) {
            model.addAttribute("errorMessage", "이메일을 입력해주세요.");
            return "user/forgot_password";
        }

        passwordResetService.requestReset(email);

        model.addAttribute("email", email);
        return "user/forgot_password_done";
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
