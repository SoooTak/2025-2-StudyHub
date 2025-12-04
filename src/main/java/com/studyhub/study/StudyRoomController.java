package com.studyhub.study;

import com.studyhub.board.Post;
import com.studyhub.board.PostRepository;
import com.studyhub.board.PostType;
import com.studyhub.user.User;
import com.studyhub.user.UserRole;
import com.studyhub.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/room")
@RequiredArgsConstructor
public class StudyRoomController {

    private final StudyRepository studyRepository;
    private final MembershipRepository membershipRepository;
    private final StudySessionRepository studySessionRepository;
    private final StudyFileRepository studyFileRepository;
    private final AttendanceRepository attendanceRepository;
    private final PostRepository postRepository;
    private final UserService userService;

    /**
     * 스터디 룸 홈 대시보드
     * - 다음 예정 세션
     * - 내 출석 요약
     * - 최근 공지글 3개
     * - 최근 자료 3개
     */
    @GetMapping("/{studyId}")
    public String home(@PathVariable("studyId") Long studyId, Model model) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        Study study = studyRepository.findById(studyId).orElse(null);
        if (study == null) {
            return "redirect:/studies";
        }

        boolean isAdmin = (currentUser.getRole() == UserRole.ADMIN);

        // 멤버십 체크
        Membership myMembership = membershipRepository.findByStudyAndUser(study, currentUser).orElse(null);

        boolean isMember = (myMembership != null) || isAdmin;
        boolean isLeader = false;
        if (myMembership != null && myMembership.getRole() == MembershipRole.LEADER) {
            isLeader = true;
        }
        // ADMIN도 리더와 비슷한 권한으로 본다고 가정
        if (isAdmin) {
            isLeader = true;
        }

        // 스터디 멤버가 아니고 ADMIN도 아니면 접근 불가
        if (!isMember) {
            return "redirect:/studies/" + studyId;
        }

        // 공통 헤더 정보 (GlobalHeaderAdvice도 있지만, 여기서 한 번 더 보장)
        prepareCommonModel(model, currentUser);

        // ===== 1) 세션 / 출석 요약 =====
        List<StudySession> sessions = studySessionRepository.findByStudyOrderBySessionDateTimeAsc(study);
        int totalSessions = sessions.size();

        // 다음 예정 세션
        StudySession nextSession = null;
        LocalDateTime now = LocalDateTime.now();
        for (StudySession session : sessions) {
            LocalDateTime dt = session.getSessionDateTime();
            if (dt != null && (dt.isAfter(now) || dt.isEqual(now))) {
                nextSession = session;
                break;
            }
        }

        // 내 출석 요약 (멤버일 때만 의미 있음, ADMIN은 제외)
        int attendedSessions = 0;
        Double attendanceRate = null;

        if (myMembership != null && totalSessions > 0) {
            for (StudySession session : sessions) {
                var attendanceOpt = attendanceRepository.findBySessionAndUser(session, currentUser);
                if (attendanceOpt.isPresent()) {
                    Attendance attendance = attendanceOpt.get();
                    AttendanceStatus status = attendance.getStatus();
                    // PRESENT, LATE 둘 다 "출석한 것"으로 간주
                    if (status == AttendanceStatus.PRESENT || status == AttendanceStatus.LATE) {
                        attendedSessions++;
                    }
                }
            }
            attendanceRate = Math.round((attendedSessions * 100.0 / totalSessions) * 10.0) / 10.0;
        }

        // ===== 2) 최근 공지글 3개 =====
        List<Post> noticePosts = postRepository.findByStudyAndTypeAndDeletedFalseOrderByCreatedAtDesc(study,
                PostType.NOTICE);
        List<Post> recentNotices = noticePosts.size() > 3 ? noticePosts.subList(0, 3) : noticePosts;
        // ===== 2-1) 최근 일반 게시글 3개 =====
        List<Post> normalPosts = postRepository.findByStudyAndTypeAndDeletedFalseOrderByCreatedAtDesc(study,
                PostType.NORMAL);
        List<Post> recentNormalPosts = normalPosts.size() > 3 ? normalPosts.subList(0, 3) : normalPosts;
        model.addAttribute("recentNormalPosts", recentNormalPosts);
        model.addAttribute("hasNormalPosts", !recentNormalPosts.isEmpty());

        // ===== 3) 최근 자료 3개 =====
        List<StudyFile> files = studyFileRepository.findByStudyOrderByUploadedAtDesc(study);
        List<StudyFile> recentFiles = files.size() > 3 ? files.subList(0, 3) : files;
        long totalFiles = studyFileRepository.countByStudy(study);

        // 화면에 내려줄 데이터
        model.addAttribute("study", study);
        model.addAttribute("isLeader", isLeader);
        model.addAttribute("isMember", isMember);
        model.addAttribute("isAdmin", isAdmin);

        // 세션/출석
        model.addAttribute("hasSessions", !sessions.isEmpty());
        model.addAttribute("totalSessions", totalSessions);
        model.addAttribute("nextSession", nextSession);
        model.addAttribute("attendedSessions", attendedSessions);
        model.addAttribute("attendanceRate", attendanceRate);

        // 공지/자료
        model.addAttribute("recentNotices", recentNotices);
        model.addAttribute("hasNotices", !recentNotices.isEmpty());
        model.addAttribute("recentFiles", recentFiles);
        model.addAttribute("hasFiles", !recentFiles.isEmpty());
        model.addAttribute("totalFiles", totalFiles);

        return "room/home";
    }

    private void prepareCommonModel(Model model, User currentUser) {
        boolean loggedIn = currentUser != null;
        model.addAttribute("loggedIn", loggedIn);
        if (loggedIn) {
            model.addAttribute("loginEmail", currentUser.getEmail());
        }
    }
}
