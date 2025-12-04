package com.studyhub.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/my/profile")
public class MyProfileController {

    private final UserService userService;
    private final UserRepository userRepository;

    /**
     * 마이페이지 조회
     */
    @GetMapping
    public String showProfile(Model model) {
        User user = userService.getCurrentUser();
        if (user == null) {
            return "redirect:/login";
        }

        UserProfileForm form = new UserProfileForm();
        form.setName(user.getName());
        form.setNickname(user.getNickname());
        form.setPhone(user.getPhone());
        form.setIntro(user.getIntro());

        model.addAttribute("form", form);
        // loginEmail, unreadCount 등은 GlobalModelAttributes에서 이미 넣어줌
        return "user/profile";
    }

    /**
     * 마이페이지 수정
     */
    @PostMapping
    public String updateProfile(@ModelAttribute("form") UserProfileForm form,
            Model model) {

        User user = userService.getCurrentUser();
        if (user == null) {
            return "redirect:/login";
        }

        if (isBlank(form.getName())) {
            model.addAttribute("errorMessage", "이름은 비워둘 수 없습니다.");
            // 에러 시에도 기존 값 유지
            model.addAttribute("form", form);
            return "user/profile";
        }

        user.setName(form.getName());
        user.setNickname(form.getNickname());
        user.setPhone(form.getPhone());
        user.setIntro(form.getIntro());

        userRepository.save(user);

        return "redirect:/my/profile";
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
