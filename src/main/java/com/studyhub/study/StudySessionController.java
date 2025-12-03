package com.studyhub.study;

import com.studyhub.user.User;
import com.studyhub.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/room")
@RequiredArgsConstructor
public class StudySessionController {

    private final StudyRepository studyRepository;
    private final MembershipRepository membershipRepository;
    private final StudySessionRepository studySessionRepository;
    private final AttendanceRepository attendanceRepository;
    private final UserService userService;

    /**
     * 세션 목록 + 내 출석 상태 보기 + 출석 요약
     * GET /room/{id}/sessions
     */
    @GetMapping("/{id}/sessions")
    public String listSessions(@PathVariable("id") Long studyId, Model model) {

        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        Study study = studyRepository.findById(studyId).orElse(null);
        if (study == null) {
            return "redirect:/studies";
        }

        boolean isLeader = study.getLeader() != null
                && study.getLeader().getId().equals(currentUser.getId());
        boolean isMember = membershipRepository.existsByStudyAndUser(study, currentUser);

        // 리더도 아니고 멤버도 아니면 접근 불가
        if (!isLeader && !isMember) {
            return "redirect:/studies/" + studyId;
        }

        // 세션 목록 불러오기
        List<StudySession> sessions = studySessionRepository.findByStudyOrderBySessionDateTimeAsc(study);

        // 내 출석 상태 맵 (세션 ID -> 상태)
        Map<Long, AttendanceStatus> myStatusMap = new HashMap<>();
        for (StudySession studySession : sessions) {
            attendanceRepository.findBySessionAndUser(studySession, currentUser)
                    .ifPresent(att -> myStatusMap.put(studySession.getId(), att.getStatus()));
        }

        // --- 출석 요약 계산 ---
        int totalSessions = sessions.size();
        long attendedSessions = myStatusMap.values().stream()
                .filter(status -> status == AttendanceStatus.PRESENT)
                .count();
        int attendanceRate = totalSessions == 0 ? 0
                : (int) Math.round(attendedSessions * 100.0 / totalSessions);

        // 공통 헤더용
        prepareCommonModel(model, currentUser);

        model.addAttribute("study", study);
        model.addAttribute("isLeader", isLeader);
        model.addAttribute("isMember", isMember);
        model.addAttribute("sessions", sessions);
        model.addAttribute("myStatusMap", myStatusMap);
        model.addAttribute("totalSessions", totalSessions);
        model.addAttribute("attendedSessions", attendedSessions);
        model.addAttribute("attendanceRate", attendanceRate);

        // 세션 생성 폼
        model.addAttribute("sessionForm", new StudySessionForm());

        return "room/sessions";
    }

    /**
     * 리더가 세션 추가
     * POST /room/{id}/sessions/new
     */
    @PostMapping("/{id}/sessions/new")
    public String createSession(@PathVariable("id") Long studyId,
            @ModelAttribute("sessionForm") StudySessionForm form) {

        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        Study study = studyRepository.findById(studyId).orElse(null);
        if (study == null) {
            return "redirect:/studies";
        }

        boolean isLeader = study.getLeader() != null
                && study.getLeader().getId().equals(currentUser.getId());

        // 멤버는 세션 생성 불가, 리더만 가능
        if (!isLeader) {
            return "redirect:/room/" + studyId + "/sessions";
        }

        // 아주 간단한 유효성 체크 (브라우저 required로 대부분 막힘)
        if (form.getTitle() == null || form.getTitle().isBlank()
                || form.getSessionDateTime() == null) {
            return "redirect:/room/" + studyId + "/sessions";
        }

        StudySession studySession = new StudySession();
        studySession.setStudy(study);
        studySession.setTitle(form.getTitle());
        studySession.setSessionDateTime(form.getSessionDateTime());
        studySession.setLocation(form.getLocation());

        studySessionRepository.save(studySession);

        return "redirect:/room/" + studyId + "/sessions";
    }

    /**
     * 출석 체크 (현재는 PRESENT로 고정)
     * POST /room/{studyId}/sessions/{sessionId}/checkin
     */
    @PostMapping("/{studyId}/sessions/{sessionId}/checkin")
    public String checkIn(@PathVariable("studyId") Long studyId,
            @PathVariable("sessionId") Long sessionId) {

        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        Study study = studyRepository.findById(studyId).orElse(null);
        if (study == null) {
            return "redirect:/studies";
        }

        boolean isLeader = study.getLeader() != null
                && study.getLeader().getId().equals(currentUser.getId());
        boolean isMember = membershipRepository.existsByStudyAndUser(study, currentUser);

        if (!isLeader && !isMember) {
            return "redirect:/studies/" + studyId;
        }

        StudySession studySession = studySessionRepository.findById(sessionId).orElse(null);
        if (studySession == null || !studySession.getStudy().getId().equals(studyId)) {
            // 세션이 없거나 다른 스터디 소속이면 그냥 목록으로
            return "redirect:/room/" + studyId + "/sessions";
        }

        // 기존 기록이 있으면 가져오고, 없으면 새로 생성
        Attendance attendance = attendanceRepository
                .findBySessionAndUser(studySession, currentUser)
                .orElseGet(() -> {
                    Attendance a = new Attendance();
                    a.setSession(studySession);
                    a.setUser(currentUser);
                    return a;
                });

        attendance.setStatus(AttendanceStatus.PRESENT); // 출석
        attendance.setCheckedAt(LocalDateTime.now());
        attendanceRepository.save(attendance);

        return "redirect:/room/" + studyId + "/sessions";
    }

    /**
     * 리더용 출석부 화면
     * GET /room/{studyId}/sessions/{sessionId}/attendance
     */
    @GetMapping("/{studyId}/sessions/{sessionId}/attendance")
    public String viewAttendanceBoard(@PathVariable("studyId") Long studyId,
            @PathVariable("sessionId") Long sessionId,
            Model model) {

        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        Study study = studyRepository.findById(studyId).orElse(null);
        if (study == null) {
            return "redirect:/studies";
        }

        boolean isLeader = study.getLeader() != null
                && study.getLeader().getId().equals(currentUser.getId());

        // 리더만 출석부 볼 수 있음
        if (!isLeader) {
            return "redirect:/room/" + studyId + "/sessions";
        }

        StudySession studySession = studySessionRepository.findById(sessionId).orElse(null);
        if (studySession == null || !studySession.getStudy().getId().equals(studyId)) {
            return "redirect:/room/" + studyId + "/sessions";
        }

        // 이 스터디의 모든 멤버 목록
        List<Membership> members = membershipRepository.findByStudy(study);

        // 이 세션의 출석 정보
        List<Attendance> attendanceList = attendanceRepository.findBySession(studySession);

        // userId -> 상태 문자열("PRESENT"/"LATE"/"ABSENT") 매핑
        Map<Long, String> memberStatusMap = new HashMap<>();
        for (Attendance att : attendanceList) {
            if (att.getUser() != null && att.getStatus() != null) {
                memberStatusMap.put(att.getUser().getId(), att.getStatus().name());
            }
        }

        prepareCommonModel(model, currentUser);

        model.addAttribute("study", study);
        model.addAttribute("studySession", studySession);
        model.addAttribute("isLeader", isLeader);
        model.addAttribute("members", members);
        model.addAttribute("memberStatusMap", memberStatusMap);

        return "room/session_attendance";
    }

    private void prepareCommonModel(Model model, User currentUser) {
        boolean loggedIn = currentUser != null;
        model.addAttribute("loggedIn", loggedIn);
        if (loggedIn) {
            model.addAttribute("loginEmail", currentUser.getEmail());
        }
    }
}
