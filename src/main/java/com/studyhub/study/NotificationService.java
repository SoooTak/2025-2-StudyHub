package com.studyhub.study;

import com.studyhub.board.Post;
import com.studyhub.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final MembershipRepository membershipRepository;

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

    /**
     * 자료실 - 파일 업로드 알림 생성
     *
     * StudyFileController 에서 파일이 정상 업로드된 뒤에 호출:
     * notificationService.createStudyFileUploadedNotifications(study, currentUser,
     * file.getOriginalFilename());
     */
    public void createStudyFileUploadedNotifications(Study study, User uploader, String originalFilename) {
        if (study == null || uploader == null) {
            return;
        }

        // 이 스터디에 속한 모든 멤버 가져오기
        List<Membership> memberships = membershipRepository.findByStudy(study);
        if (memberships == null || memberships.isEmpty()) {
            return;
        }

        String safeFilename = (originalFilename != null && !originalFilename.isBlank())
                ? originalFilename
                : "새 자료";
        String studyTitle = (study.getTitle() != null && !study.getTitle().isBlank())
                ? study.getTitle()
                : "스터디";

        List<Notification> notifications = new ArrayList<>();

        for (Membership membership : memberships) {
            if (membership == null || membership.getUser() == null) {
                continue;
            }

            User targetUser = membership.getUser();

            // 업로더 본인에게는 굳이 알림을 보내지 않음 (원하면 지워도 됨)
            if (targetUser.getId().equals(uploader.getId())) {
                continue;
            }

            Notification n = new Notification();
            n.setUser(targetUser);
            n.setStudy(study);

            // ⚠️ 타입은 현재 APPLICATION_APPROVED를 재사용하고 있음.
            // 이미 NotificationType에 STUDY_FILE 같은 타입을 만들어두었다면
            // 여기만 그 타입으로 바꿔주면 됨.
            n.setType(NotificationType.STUDY_FILE_UPLOADED);

            n.setMessage("[" + studyTitle + "] 새 자료가 업로드되었습니다: " + safeFilename);
            n.setLinkUrl("/room/" + study.getId() + "/files");

            notifications.add(n);
        }

        if (!notifications.isEmpty()) {
            notificationRepository.saveAll(notifications);
        }
    }

    @Transactional(readOnly = true)
    public List<Notification> getNotificationsFor(User user) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user);
    }

    @Transactional(readOnly = true)
    public List<Notification> getUnreadNotificationsFor(User user) {
        return notificationRepository.findByUserAndReadFalseOrderByCreatedAtDesc(user);
    }

    /**
     * 읽지 않은 알림 개수
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(User user) {
        if (user == null) {
            return 0L;
        }
        return notificationRepository.countByUserAndReadFalse(user);
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
    
    public void createPostCreatedNotifications(Study study, Post post, User author) {
        List<Membership> members = membershipRepository.findByStudy(study);

        for (Membership m : members) {
            if (m.getUser().getId().equals(author.getId()))
                continue;

            Notification n = new Notification();
            n.setUser(m.getUser());
            n.setStudy(study);
            n.setType(NotificationType.POST_CREATED);
            n.setMessage("[" + study.getTitle() + "] 새로운 게시글: " + post.getTitle());
            n.setLinkUrl("/room/" + study.getId() + "/board/" + post.getId());
            n.setRead(false);

            notificationRepository.save(n);
        }
    }
    
    public void createCommentCreatedNotification(Study study, Post post, User commentWriter) {
        User postWriter = post.getWriter();

        if (postWriter.getId().equals(commentWriter.getId()))
            return;

        Notification n = new Notification();
        n.setUser(postWriter);
        n.setStudy(study);
        n.setType(NotificationType.COMMENT_CREATED);
        n.setMessage("[" + study.getTitle() + "] 내 게시글에 새 댓글이 달렸습니다.");
        n.setLinkUrl("/room/" + study.getId() + "/board/" + post.getId());
        n.setRead(false);

        notificationRepository.save(n);
    }
    
    public void createAttendanceChangedNotification(
            Study study,
            StudySession session,
            User targetUser,
            AttendanceStatus oldStatus,
            AttendanceStatus newStatus) {
        Notification n = new Notification();
        n.setUser(targetUser);
        n.setStudy(study);
        n.setType(NotificationType.ATTENDANCE_CHANGED);

        String msg = "[" + study.getTitle() + "] "
                + session.getTitle() + " 출석 변경: "
                + oldStatus + " → " + newStatus;

        n.setMessage(msg);
        n.setLinkUrl("/room/" + study.getId() + "/sessions");
        n.setRead(false);

        notificationRepository.save(n);
    }
    
    // 새 가입 신청이 들어왔을 때, 리더/매니저에게 알림 생성
    public void createApplicationSubmittedNotifications(Application application) {
        if (application == null) {
            return;
        }
        Study study = application.getStudy();
        User applicant = application.getApplicant();
        if (study == null || applicant == null) {
            return;
        }

        // 스터디의 리더 + 매니저 목록 조회
        List<Membership> targets = membershipRepository.findByStudy(study).stream()
                .filter(m -> m.getRole() == MembershipRole.LEADER || m.getRole() == MembershipRole.MANAGER)
                // 신청자 본인에게는 이 알림 보내지 않기
                .filter(m -> !m.getUser().getId().equals(applicant.getId()))
                .toList();

        String message = String.format(
                "[%s] 새 가입 신청이 도착했습니다: %s",
                study.getTitle(),
                applicant.getNickname() != null ? applicant.getNickname() : applicant.getName());

        String linkUrl = "/my/studies/" + study.getId() + "/applications";

        for (Membership membership : targets) {
            Notification notification = new Notification();
            notification.setUser(membership.getUser());
            notification.setStudy(study);
            notification.setType(NotificationType.APPLICATION_SUBMITTED);
            notification.setMessage(message);
            notification.setLinkUrl(linkUrl);
            notificationRepository.save(notification);
        }
    }
}
