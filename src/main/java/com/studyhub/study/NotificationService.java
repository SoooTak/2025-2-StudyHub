package com.studyhub.study;

import com.studyhub.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;

    /**
     * 가입 신청 승인/거절 결과 알림 생성
     */
    public void createApplicationResultNotification(Application application) {
        if (application == null || application.getApplicant() == null || application.getStudy() == null) {
            return;
        }

        if (application.getStatus() != ApplicationStatus.APPROVED &&
                application.getStatus() != ApplicationStatus.REJECTED) {
            // 최종 결과(승인/거절)가 아닐 때는 알림을 만들지 않음
            return;
        }

        Notification notification = new Notification();
        notification.setUser(application.getApplicant());
        notification.setStudy(application.getStudy());

        if (application.getStatus() == ApplicationStatus.APPROVED) {
            notification.setType(NotificationType.APPLICATION_APPROVED);
            notification.setMessage("[" + application.getStudy().getTitle() + "] 가입 신청이 승인되었습니다.");
            // 바로 스터디 룸으로 이동
            notification.setLinkUrl("/room/" + application.getStudy().getId());
        } else {
            notification.setType(NotificationType.APPLICATION_REJECTED);
            notification.setMessage("[" + application.getStudy().getTitle() + "] 가입 신청이 거절되었습니다.");
            // 스터디 상세로 이동
            notification.setLinkUrl("/studies/" + application.getStudy().getId());
        }

        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<Notification> getNotificationsFor(User user) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user);
    }

    @Transactional(readOnly = true)
    public List<Notification> getUnreadNotificationsFor(User user) {
        return notificationRepository.findByUserAndReadFalseOrderByCreatedAtDesc(user);
    }

    public Notification markAsRead(Long id, User user) {
        return notificationRepository.findByIdAndUser(id, user)
                .map(n -> {
                    if (!n.isRead()) {
                        n.setRead(true);
                        notificationRepository.save(n);
                    }
                    return n;
                })
                .orElse(null);
    }

    public void markAllAsRead(User user) {
        List<Notification> unread = notificationRepository.findByUserAndReadFalseOrderByCreatedAtDesc(user);
        for (Notification n : unread) {
            n.setRead(true);
        }
        if (!unread.isEmpty()) {
            notificationRepository.saveAll(unread);
        }
    }
}
