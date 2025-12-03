package com.studyhub.study;

import com.studyhub.user.User;
import com.studyhub.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/my/studies")
@RequiredArgsConstructor
public class MyStudyController {

    private final StudyRepository studyRepository;
    private final ApplicationRepository applicationRepository;
    private final MembershipRepository membershipRepository;
    private final UserService userService;
    private final NotificationService notificationService;

    /**
     * 내 스터디 메인
     * - 내가 리더인 스터디 목록
     * - 내가 멤버(MEMBER) 또는 매니저(MANAGER)로 참여 중인 스터디 목록
     */
    @GetMapping
    public String listMyStudies(Model model) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        // 내가 리더(개설자)인 스터디
        List<Study> leaderStudies = studyRepository.findByLeader(currentUser);

        // 내가 멤버로 참여 중인 스터디
        List<Membership> memberMemberships = membershipRepository.findByUserAndRole(currentUser, MembershipRole.MEMBER);

        // 내가 매니저로 참여 중인 스터디
        List<Membership> managerMemberships = membershipRepository.findByUserAndRole(currentUser,
                MembershipRole.MANAGER);

        // MEMBER + MANAGER 를 하나의 리스트로 합치기
        List<Membership> joinedMemberships = new ArrayList<>();
        joinedMemberships.addAll(memberMemberships);
        joinedMemberships.addAll(managerMemberships);

        model.addAttribute("leaderStudies", leaderStudies);
        model.addAttribute("joinedMemberships", joinedMemberships);

        // 헤더 표시용
        model.addAttribute("loggedIn", true);
        model.addAttribute("loginEmail", currentUser.getEmail());

        return "mystudies/list";
    }

    /**
     * 스터디 개설 폼 (리더가 될 사용자)
     * GET /my/studies/new
     */
    @GetMapping("/new")
    public String newStudyForm(Model model) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        StudyForm form = new StudyForm();
        form.setOpen(true);
        form.setMaxMembers(10); // 기본값 10명

        model.addAttribute("form", form);
        model.addAttribute("mode", "create"); // create / edit 구분용
        model.addAttribute("pageTitle", "스터디 개설");

        model.addAttribute("loggedIn", true);
        model.addAttribute("loginEmail", currentUser.getEmail());

        return "mystudies/form";
    }

    /**
     * 스터디 개설 처리
     * POST /my/studies/new
     */
    @PostMapping("/new")
    public String createStudy(@ModelAttribute("form") StudyForm form) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        // 제목이 비었으면 기본값 사용
        String title = (form.getTitle() == null || form.getTitle().trim().isEmpty())
                ? "제목 없는 스터디"
                : form.getTitle().trim();

        String description = form.getDescription();
        if (description == null || description.trim().isEmpty()) {
            description = "스터디 소개가 아직 작성되지 않았습니다.";
        }

        String category = form.getCategory();
        if (category == null) {
            category = "";
        }

        Integer maxMembers = form.getMaxMembers();
        if (maxMembers == null || maxMembers < 1) {
            maxMembers = 10;
        }

        // Study 엔티티 생성
        Study study = new Study();
        study.setTitle(title);
        study.setDescription(description);
        study.setCategory(category.trim().isEmpty() ? null : category.trim());
        study.setMaxMembers(maxMembers);
        study.setLeader(currentUser);

        // Study 엔티티의 공개 여부 설정
        // Study 엔티티에 isPublic()/setPublic(boolean) 형태의 메서드가 있다고 가정
        study.setPublic(form.isOpen());

        // 저장
        studyRepository.save(study);

        // 리더를 memberships에도 LEADER 역할로 추가 (이미 존재하면 추가하지 않음)
        boolean alreadyMember = membershipRepository.existsByStudyAndUser(study, currentUser);
        if (!alreadyMember) {
            Membership leaderMembership = new Membership();
            leaderMembership.setStudy(study);
            leaderMembership.setUser(currentUser);
            leaderMembership.setRole(MembershipRole.LEADER);
            membershipRepository.save(leaderMembership);
        }

        // 개설 후 해당 스터디 홈으로 이동
        return "redirect:/room/" + study.getId();
    }

    /**
     * 스터디 정보 수정 폼 (리더 전용)
     * GET /my/studies/{id}/edit
     */
    @GetMapping("/{id}/edit")
    public String editStudyForm(@PathVariable("id") Long studyId, Model model) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        Study study = studyRepository.findById(studyId).orElse(null);
        if (study == null) {
            return "redirect:/my/studies";
        }

        // 리더만 수정 가능
        if (!study.getLeader().getId().equals(currentUser.getId())) {
            return "redirect:/my/studies";
        }

        StudyForm form = new StudyForm();
        form.setId(study.getId());
        form.setTitle(study.getTitle());
        form.setDescription(study.getDescription());
        form.setCategory(study.getCategory());
        form.setMaxMembers(study.getMaxMembers() != null ? study.getMaxMembers() : 10);
        form.setOpen(study.isPublic()); // Study 엔티티의 isPublic() 사용

        model.addAttribute("form", form);
        model.addAttribute("mode", "edit");
        model.addAttribute("pageTitle", "스터디 정보 수정");

        model.addAttribute("loggedIn", true);
        model.addAttribute("loginEmail", currentUser.getEmail());

        return "mystudies/form";
    }

    /**
     * 스터디 정보 수정 처리 (리더 전용)
     * POST /my/studies/{id}/edit
     */
    @PostMapping("/{id}/edit")
    public String updateStudy(@PathVariable("id") Long studyId,
            @ModelAttribute("form") StudyForm form) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        Study study = studyRepository.findById(studyId).orElse(null);
        if (study == null) {
            return "redirect:/my/studies";
        }

        // 리더만 수정 가능
        if (!study.getLeader().getId().equals(currentUser.getId())) {
            return "redirect:/my/studies";
        }

        // 제목/소개/카테고리/인원/공개 여부 업데이트
        String title = (form.getTitle() == null || form.getTitle().trim().isEmpty())
                ? "제목 없는 스터디"
                : form.getTitle().trim();

        String description = form.getDescription();
        if (description == null || description.trim().isEmpty()) {
            description = "스터디 소개가 아직 작성되지 않았습니다.";
        }

        String category = form.getCategory();
        if (category == null) {
            category = "";
        }

        Integer maxMembers = form.getMaxMembers();
        if (maxMembers == null || maxMembers < 1) {
            maxMembers = 10;
        }

        study.setTitle(title);
        study.setDescription(description);
        study.setCategory(category.trim().isEmpty() ? null : category.trim());
        study.setMaxMembers(maxMembers);
        study.setPublic(form.isOpen());

        studyRepository.save(study);

        // 수정 후 스터디 홈으로 이동
        return "redirect:/room/" + study.getId();
    }

    /**
     * 특정 스터디의 가입 신청 목록 (리더 전용)
     * GET /my/studies/{id}/applications
     */
    @GetMapping("/{id}/applications")
    public String viewApplications(@PathVariable("id") Long studyId, Model model) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        Study study = studyRepository.findById(studyId).orElse(null);
        if (study == null) {
            return "redirect:/my/studies";
        }

        // 리더가 아닌 경우 접근 불가
        if (!study.getLeader().getId().equals(currentUser.getId())) {
            return "redirect:/my/studies";
        }

        // PENDING 상태의 신청들만 보기
        List<Application> pendingApplications = applicationRepository.findByStudyAndStatusOrderByIdDesc(study,
                ApplicationStatus.PENDING);

        // 전체 신청들 (승인/거절 포함)
        List<Application> allApplications = applicationRepository.findByStudyOrderByIdDesc(study);

        model.addAttribute("study", study);
        model.addAttribute("pendingApplications", pendingApplications);
        model.addAttribute("allApplications", allApplications);

        // 헤더 표시용
        model.addAttribute("loggedIn", true);
        model.addAttribute("loginEmail", currentUser.getEmail());

        return "mystudies/applications";
    }

    /**
     * 가입 신청 승인 처리 (리더 전용)
     * POST /my/studies/{id}/applications/{appId}/approve
     */
    @PostMapping("/{id}/applications/{appId}/approve")
    public String approveApplication(@PathVariable("id") Long studyId,
            @PathVariable("appId") Long applicationId) {

        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        Study study = studyRepository.findById(studyId).orElse(null);
        if (study == null) {
            return "redirect:/my/studies";
        }

        // 리더가 아닌 경우 접근 불가
        if (!study.getLeader().getId().equals(currentUser.getId())) {
            return "redirect:/my/studies";
        }

        Application application = applicationRepository.findById(applicationId).orElse(null);
        if (application == null || !application.getStudy().getId().equals(studyId)) {
            return "redirect:/my/studies/" + studyId + "/applications";
        }

        application.setStatus(ApplicationStatus.APPROVED);
        applicationRepository.save(application);

        // 이미 멤버인지 확인 후, 아니면 MEMBER로 추가
        User applicant = application.getApplicant();
        boolean alreadyMember = membershipRepository.existsByStudyAndUser(study, applicant);
        if (!alreadyMember) {
            Membership membership = new Membership();
            membership.setStudy(study);
            membership.setUser(applicant);
            membership.setRole(MembershipRole.MEMBER);
            membershipRepository.save(membership);
        }

        // 알림 생성
        notificationService.createApplicationResultNotification(application);

        return "redirect:/my/studies/" + studyId + "/applications";
    }

    /**
     * 가입 신청 거절 처리 (리더 전용)
     * POST /my/studies/{id}/applications/{appId}/reject
     */
    @PostMapping("/{id}/applications/{appId}/reject")
    public String rejectApplication(@PathVariable("id") Long studyId,
            @PathVariable("appId") Long applicationId) {

        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        Study study = studyRepository.findById(studyId).orElse(null);
        if (study == null) {
            return "redirect:/my/studies";
        }

        // 리더가 아닌 경우 접근 불가
        if (!study.getLeader().getId().equals(currentUser.getId())) {
            return "redirect:/my/studies";
        }

        Application application = applicationRepository.findById(applicationId).orElse(null);
        if (application == null || !application.getStudy().getId().equals(studyId)) {
            return "redirect:/my/studies/" + studyId + "/applications";
        }

        application.setStatus(ApplicationStatus.REJECTED);
        applicationRepository.save(application);

        // 알림 생성
        notificationService.createApplicationResultNotification(application);

        return "redirect:/my/studies/" + studyId + "/applications";
    }
}
