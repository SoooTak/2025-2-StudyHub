package com.studyhub.config;

import com.studyhub.study.NotificationService; // ✅ 패키지 주의
import com.studyhub.user.User;
import com.studyhub.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.ui.Model;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAttributes {

    private final UserService userService;
    private final NotificationService notificationService;

    @ModelAttribute
    public void addGlobalAttributes(Model model) {
        User user = userService.getCurrentUser();

        boolean loggedIn = (user != null);
        String loginEmail = loggedIn ? user.getEmail() : null;
        long unreadCount = loggedIn ? notificationService.getUnreadCount(user) : 0L; // ✅ long으로 받기

        model.addAttribute("loggedIn", loggedIn);
        model.addAttribute("loginEmail", loginEmail);
        model.addAttribute("unreadCount", unreadCount); // ✅ Long/long 그대로 넣어도 됨
    }
}
