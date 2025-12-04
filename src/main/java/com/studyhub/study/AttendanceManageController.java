package com.studyhub.study;

import com.studyhub.user.User;
import com.studyhub.user.UserRepository;
import com.studyhub.user.UserService;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/room/{studyId}/sessions/{sessionId}")
@RequiredArgsConstructor
public class AttendanceManageController {

    private final StudyRepository studyRepository;
    private final StudySessionRepository studySessionRepository;
    private final AttendanceRepository attendanceRepository;
    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository; // ⭐ UserService 대신 UserRepository 직접 사용
    private final UserService userService;
    private final NotificationService notificationService;

    /**
     * 출석 상태 변경 (리더 or 매니저만)
     */
    @PostMapping("/attendance/{userId}/update")
public String updateAttendance(@PathVariable Long studyId,
                               @PathVariable Long sessionId,
                               @PathVariable Long userId,
                               @RequestParam("status") AttendanceStatus newStatus) {

    User currentUser = userService.getCurrentUser();
    if (currentUser == null) {
        return "redirect:/login";
    }

    Study study = studyRepository.findById(studyId).orElse(null);
    if (study == null) {
        return "redirect:/studies";
    }

    Membership membership = membershipRepository.findByStudyAndUser(study, currentUser).orElse(null);
    if (membership == null) {
        return "redirect:/studies/" + studyId;
    }

    boolean isLeader = membership.getRole() == MembershipRole.LEADER;
    boolean isManager = membership.getRole() == MembershipRole.MANAGER;

    if (!isLeader && !isManager) {
        return "redirect:/room/" + studyId + "/sessions";
    }

    StudySession session = studySessionRepository.findById(sessionId).orElse(null);
    if (session == null || !session.getStudy().getId().equals(studyId)) {
        return "redirect:/room/" + studyId + "/sessions";
    }

    User targetUser = userRepository.findById(userId).orElse(null);
    if (targetUser == null) {
        return "redirect:/room/" + studyId + "/sessions";
    }

    Attendance attendance = attendanceRepository
            .findBySessionAndUser(session, targetUser)
            .orElse(null);

    if (attendance == null) {
        attendance = new Attendance();
        attendance.setSession(session);
        attendance.setUser(targetUser);
        attendance.setStatus(newStatus);
        attendance.setCheckedAt(LocalDateTime.now());
        attendanceRepository.save(attendance);

        // 최초 생성이라 oldStatus 없음 → 알림은 new → new?
        notificationService.createAttendanceChangedNotification(
                study, session, targetUser, newStatus, newStatus
        );
        return "redirect:/room/" + studyId + "/sessions";
    }

    // ⭐ 기존 상태 저장
    AttendanceStatus oldStatus = attendance.getStatus();

    attendance.setStatus(newStatus);
    attendance.setCheckedAt(LocalDateTime.now());
    attendanceRepository.save(attendance);

    // ⭐ 알림 생성
    notificationService.createAttendanceChangedNotification(
            study, session, targetUser, oldStatus, newStatus
    );

    return "redirect:/room/" + studyId + "/sessions";
}
}
