package com.studyhub.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    @GetMapping("/reset-password")
    public String showResetForm(@RequestParam("token") String token,
            Model model) {

        var resetToken = passwordResetService.validateToken(token);
        if (resetToken == null) {
            model.addAttribute("invalid", true);
            return "user/reset_password";
        }
        model.addAttribute("invalid", false);
        model.addAttribute("token", token);
        return "user/reset_password";
    }

    @PostMapping("/reset-password")
    public String handleReset(@RequestParam("token") String token,
            @RequestParam("password") String password,
            @RequestParam("confirmPassword") String confirmPassword,
            Model model) {

        if (password == null || password.isBlank()
                || confirmPassword == null || confirmPassword.isBlank()) {
            model.addAttribute("invalid", false);
            model.addAttribute("token", token);
            model.addAttribute("errorMessage", "비밀번호를 입력해주세요.");
            return "user/reset_password";
        }

        if (!password.equals(confirmPassword)) {
            model.addAttribute("invalid", false);
            model.addAttribute("token", token);
            model.addAttribute("errorMessage", "비밀번호와 비밀번호 확인이 일치하지 않습니다.");
            return "user/reset_password";
        }

        boolean success = passwordResetService.resetPassword(token, password);
        model.addAttribute("resetSuccess", success);
        return "user/reset_password";
    }
}
