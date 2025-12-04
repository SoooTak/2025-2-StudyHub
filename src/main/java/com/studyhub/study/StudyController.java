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
    private final NotificationService notificationService;

    /**
     * 공개 스터디 목록 + 검색/필터/정렬
     */
    @GetMapping("/studies")
    public String listStudies(@RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "sort", required = false) String sort,
            Model model) {

        // 로그인 상태 정보
        User currentUser = userService.getCurrentUser();
        boolean loggedIn = (currentUser != null);
        model.addAttribute("loggedIn", loggedIn);
        if (loggedIn) {
            model.addAttribute("loginEmail", currentUser.getEmail());
        }

        // 공개 스터디 기본 목록 (최신순)
        List<Study> studies = studyRepository.findByIsPublicTrueOrderByCreatedAtDesc();

        // 검색어 필터 (제목 + 소개)
        if (keyword != null && !keyword.isBlank()) {
            String lowered = keyword.toLowerCase();
            studies = studies.stream()
                    .filter(s -> (s.getTitle() != null && s.getTitle().toLowerCase().contains(lowered)) ||
                            (s.getDescription() != null && s.getDescription().toLowerCase().contains(lowered)))
                    .toList();
        }

        // 카테고리 필터 (부분 일치)
        if (category != null && !category.isBlank()) {
            String loweredCategory = category.toLowerCase();
            studies = studies.stream()
                    .filter(s -> s.getCategory() != null &&
                            s.getCategory().toLowerCase().contains(loweredCategory))
                    .toList();
        }

        // 정렬 (latest, oldest, title)
        String effectiveSort = (sort == null || sort.isBlank()) ? "latest" : sort;
        switch (effectiveSort) {
            case "title":
                studies = studies.stream()
                        .sorted(java.util.Comparator.comparing(
                                Study::getTitle,
                                java.util.Comparator.nullsLast(String::compareToIgnoreCase)))
                        .toList();
                break;
            case "oldest":
                studies = studies.stream()
                        .sorted(java.util.Comparator.comparing(Study::getCreatedAt))
                        .toList();
                break;
            case "latest":
            default:
                studies = studies.stream()
                        .sorted(java.util.Comparator.comparing(Study::getCreatedAt).reversed())
                        .toList();
                effectiveSort = "latest";
                break;
        }

        // 로그인한 사용자의 참여/신청 상태 (JOINED / PENDING / NONE)
        java.util.Map<Long, String> joinStatusMap = new java.util.HashMap<>();
        if (currentUser != null) {
            for (Study study : studies) {
                String status = "NONE";
                if (membershipRepository.existsByStudyAndUser(study, currentUser)) {
                    status = "JOINED";
                } else if (applicationRepository
                        .findByStudyAndApplicantAndStatus(study, currentUser, ApplicationStatus.PENDING)
                        .isPresent()) {
                    status = "PENDING";
                }
                joinStatusMap.put(study.getId(), status);
            }
        }

        model.addAttribute("studies", studies);
        model.addAttribute("joinStatusMap", joinStatusMap);
        model.addAttribute("keyword", keyword == null ? "" : keyword);
        model.addAttribute("category", category == null ? "" : category);
        model.addAttribute("sort", effectiveSort);

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

        // 이미 멤버이면 신청 폼 볼 필요 없음 → 상세로 돌려보내기
        if (membershipRepository.existsByStudyAndUser(study, currentUser)) {
            return "redirect:/studies/" + id;
        }

        // 이미 대기중 신청이 있으면 폼 안 열어주고 상세로
        if (applicationRepository
                .findByStudyAndApplicantAndStatus(study, currentUser, ApplicationStatus.PENDING)
                .isPresent()) {
            return "redirect:/studies/" + id;
        }

        // 폼 초기값 준비
        StudyApplyForm form = new StudyApplyForm();

        model.addAttribute("study", study);
        model.addAttribute("form", form);

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
