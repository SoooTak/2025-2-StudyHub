package com.studyhub.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 회원가입 폼 화면
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
    public String handleSignup(UserRegisterForm form, Model model) {

        // 1) 필수값 검증
        if (isBlank(form.getEmail())
                || isBlank(form.getPassword())
                || isBlank(form.getConfirmPassword())
                || isBlank(form.getName())
                || isBlank(form.getNickname())) {

            model.addAttribute("errorMessage", "필수 항목을 모두 입력해 주세요.");
            model.addAttribute("form", form);
            return "user/signup";
        }

        // 2) 비밀번호 확인 일치 여부
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            model.addAttribute("errorMessage", "비밀번호와 비밀번호 확인이 일치하지 않습니다.");
            model.addAttribute("form", form);
            return "user/signup";
        }

        // 3) 이메일 중복 체크
        if (userRepository.findByEmail(form.getEmail()).isPresent()) {
            model.addAttribute("errorMessage", "이미 사용 중인 이메일입니다.");
            model.addAttribute("form", form);
            return "user/signup";
        }

        // 4) 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(form.getPassword());

        // 5) User 엔티티 생성 및 저장
        User user = User.builder()
                .email(form.getEmail())
                .password(encodedPassword)
                .name(form.getName())
                .nickname(form.getNickname())
                .phone(form.getPhone())
                .intro(form.getIntro())
                .role(UserRole.USER)
                .emailVerified(false) // 나중에 이메일 인증 기능 붙일 예정
                .build();

        userRepository.save(user);

        // 6) 회원가입 성공 화면으로 이동
        return "user/signup-success";
    }

    /**
     * 로그인 폼 화면 (실제 인증은 Spring Security가 처리)
     */
    @GetMapping("/login")
    public String showLoginForm() {
        return "user/login";
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
