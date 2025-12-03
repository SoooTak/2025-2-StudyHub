package com.studyhub.study;

import com.studyhub.user.User;
import com.studyhub.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    /**
     * 내 알림 목록
     */
    @GetMapping
    public String listNotifications(Model model) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        List<Notification> notifications = notificationService.getNotificationsFor(currentUser);
        long unreadCount = notifications.stream().filter(n -> !n.isRead()).count();

        model.addAttribute("notifications", notifications);
        model.addAttribute("unreadCount", unreadCount);

        model.addAttribute("loggedIn", true);
        model.addAttribute("loginEmail", currentUser.getEmail());

        return "notifications/list";
    }

    /**
     * 개별 알림 읽음 처리 후 링크로 이동 (링크가 없으면 목록으로)
     */
    @PostMapping("/{id}/read")
    public String readNotification(@PathVariable("id") Long id) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        Notification notification = notificationService.markAsRead(id, currentUser);
        if (notification != null && notification.getLinkUrl() != null && !notification.getLinkUrl().isBlank()) {
            return "redirect:" + notification.getLinkUrl();
        }

        return "redirect:/notifications";
    }

    /**
     * 모든 알림 읽음 처리
     */
    @PostMapping("/read-all")
    public String readAllNotifications() {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        notificationService.markAllAsRead(currentUser);
        return "redirect:/notifications";
    }
}
