package com.studyhub.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    /**
     * 이메일 인증 링크 처리
     * 예: /verify-email?token=xxxx
     */
    @GetMapping("/verify-email")
    public String verifyEmail(@RequestParam("token") String token,
            Model model) {

        boolean success = emailVerificationService.verifyToken(token);
        model.addAttribute("success", success);
        return "user/verify_email_result";
    }
}
