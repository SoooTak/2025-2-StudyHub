package com.studyhub.study;

import com.studyhub.user.User;
import com.studyhub.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class StudyController {

    private final StudyRepository studyRepository;
    private final MembershipRepository membershipRepository;
    private final ApplicationRepository applicationRepository;
    private final UserService userService;

    /**
     * 공개 스터디 목록
     */
    @GetMapping("/studies")
    public String listStudies(Model model) {
        List<Study> studies = studyRepository.findByIsPublicTrueOrderByCreatedAtDesc();
        model.addAttribute("studies", studies);

        // 로그인 상태 정보
        User currentUser = userService.getCurrentUser();
        boolean loggedIn = (currentUser != null);
        model.addAttribute("loggedIn", loggedIn);
        if (loggedIn) {
            model.addAttribute("loginEmail", currentUser.getEmail());
        }

        return "studies/list";
    }

    /**
     * 스터디 상세
     */
    @GetMapping("/studies/{id}")
    public String viewStudy(@PathVariable("id") Long id, Model model) {
        Study study = studyRepository.findById(id).orElse(null);
        if (study == null) {
            return "redirect:/studies";
        }

        User currentUser = userService.getCurrentUser();

        boolean loggedIn = (currentUser != null);
        boolean isMember = false;
        boolean hasPendingApplication = false;

        if (currentUser != null) {
            isMember = membershipRepository.existsByStudyAndUser(study, currentUser);
            hasPendingApplication = applicationRepository
                    .findByStudyAndApplicantAndStatus(study, currentUser, ApplicationStatus.PENDING)
                    .isPresent();
        }

        model.addAttribute("study", study);
        model.addAttribute("loggedIn", loggedIn);
        if (loggedIn) {
            model.addAttribute("loginEmail", currentUser.getEmail());
        }
        model.addAttribute("isMember", isMember);
        model.addAttribute("hasPendingApplication", hasPendingApplication);

        return "studies/detail";
    }

   


    /**
     * 스터디 가입 신청 폼 (로그인 필요)
     */
    @GetMapping("/my/studies/{id}/apply")
    public String showApplyForm(@PathVariable("id") Long id, Model model) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        Study study = studyRepository.findById(id).orElse(null);
        if (study == null) {
            return "redirect:/studies";
        }

        // 이미 멤버이면 신청할 수 없음
        if (membershipRepository.existsByStudyAndUser(study, currentUser)) {
            return "redirect:/studies/" + id;
        }

        // 이미 대기중인 신청이 있으면 신청할 수 없음
        if (applicationRepository
                .findByStudyAndApplicantAndStatus(study, currentUser, ApplicationStatus.PENDING)
                .isPresent()) {
            return "redirect:/studies/" + id;
        }

        model.addAttribute("study", study);
        model.addAttribute("form", new StudyApplyForm());
        return "studies/apply";
    }

    /**
     * 스터디 가입 신청 처리 (로그인 필요)
     */
    @PostMapping("/my/studies/{id}/apply")
    public String handleApply(@PathVariable("id") Long id,
            @ModelAttribute("form") StudyApplyForm form,
            Model model) {

        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        Study study = studyRepository.findById(id).orElse(null);
        if (study == null) {
            return "redirect:/studies";
        }

        // 이미 멤버이면 신청할 수 없음
        if (membershipRepository.existsByStudyAndUser(study, currentUser)) {
            return "redirect:/studies/" + id;
        }

        // 이미 대기중인 신청이 있으면 신청할 수 없음
        if (applicationRepository
                .findByStudyAndApplicantAndStatus(study, currentUser, ApplicationStatus.PENDING)
                .isPresent()) {
            return "redirect:/studies/" + id;
        }

        Application application = Application.builder()
                .study(study)
                .applicant(currentUser)
                .message(form.getMessage())
                .status(ApplicationStatus.PENDING)
                .build();

        applicationRepository.save(application);

        return "redirect:/studies/" + id;
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
