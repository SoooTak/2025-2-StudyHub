package com.studyhub.common;

import com.studyhub.study.NotificationService;
import com.studyhub.user.User;
import com.studyhub.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.ui.Model;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalHeaderAdvice {

    private final UserService userService;
    private final NotificationService notificationService;

    @ModelAttribute
    public void addGlobalAttributes(Model model) {
        User currentUser = userService.getCurrentUser();
        boolean loggedIn = (currentUser != null);

        model.addAttribute("loggedIn", loggedIn);

        if (loggedIn) {
            model.addAttribute("loginEmail", currentUser.getEmail());
            long unreadCount = notificationService.getUnreadCount(currentUser);
            model.addAttribute("unreadNotificationCount", unreadCount);
        } else {
            model.addAttribute("unreadNotificationCount", 0L);
        }
    }
}
