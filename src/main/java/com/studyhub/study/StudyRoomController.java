package com.studyhub.study;

import com.studyhub.user.User;
import com.studyhub.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/room")
@RequiredArgsConstructor
public class StudyRoomController {

    private final StudyRepository studyRepository;
    private final MembershipRepository membershipRepository;
    private final UserService userService;

    /**
     * 스터디 내부 홈 (대시보드)
     * GET /room/{id}
     * - 리더 또는 멤버만 접근 가능
     */
    @GetMapping("/{id}")
    public String studyRoom(@PathVariable("id") Long studyId, Model model) {

        // 로그인 여부 확인
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        // 스터디 조회
        Study study = studyRepository.findById(studyId).orElse(null);
        if (study == null) {
            return "redirect:/studies";
        }

        // 리더인지 확인
        boolean isLeader = study.getLeader() != null
                && study.getLeader().getId().equals(currentUser.getId());

        // 멤버인지 확인
        boolean isMember = membershipRepository.existsByStudyAndUser(study, currentUser);

        // 리더도 아니고 멤버도 아니면 접근 불가 → 상세 페이지로 돌려보냄
        if (!isLeader && !isMember) {
            return "redirect:/studies/" + studyId;
        }

        // 화면에 내려줄 데이터
        model.addAttribute("study", study);
        model.addAttribute("isLeader", isLeader);
        model.addAttribute("isMember", isMember);

        // 헤더용
        model.addAttribute("loggedIn", true);
        model.addAttribute("loginEmail", currentUser.getEmail());

        return "room/home";
    }
}
