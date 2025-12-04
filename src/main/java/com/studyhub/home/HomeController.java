// src/main/java/com/studyhub/home/HomeController.java
package com.studyhub.home;

import com.studyhub.study.Membership;
import com.studyhub.study.MembershipRepository;
import com.studyhub.study.Study;
import com.studyhub.study.StudyRepository;
import com.studyhub.user.User;
import com.studyhub.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final UserService userService;
    private final MembershipRepository membershipRepository;
    private final StudyRepository studyRepository;

    @GetMapping("/")
    public String home(Model model) {
        User currentUser = userService.getCurrentUser();
        boolean loggedIn = (currentUser != null);

        model.addAttribute("loggedIn", loggedIn);

        if (loggedIn) {
            model.addAttribute("loginEmail", currentUser.getEmail());

            // 내 스터디 목록
            List<Membership> myMemberships = membershipRepository.findByUserOrderByJoinedAtAsc(currentUser);
            model.addAttribute("myMemberships", myMemberships);
        }

        // 공개 스터디 (최신 5개)
        List<Study> publicStudies = studyRepository.findTop5ByIsPublicTrueOrderByCreatedAtDesc();
        model.addAttribute("publicStudies", publicStudies);

        return "index";
    }
}
