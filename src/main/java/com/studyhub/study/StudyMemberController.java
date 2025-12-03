package com.studyhub.study;

import com.studyhub.user.User;
import com.studyhub.user.UserRole;
import com.studyhub.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Comparator;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/room/{studyId}/members")
public class StudyMemberController {

    private final StudyRepository studyRepository;
    private final MembershipRepository membershipRepository;
    private final UserService userService;

    /**
     * 멤버 목록 화면
     */
    @GetMapping
    public String members(@PathVariable("studyId") Long studyId, Model model) {

        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        Study study = studyRepository.findById(studyId).orElse(null);
        if (study == null) {
            return "redirect:/studies";
        }

        List<Membership> memberships = membershipRepository.findByStudy(study);

        Membership myMembership = memberships.stream()
                .filter(m -> m.getUser().getId().equals(currentUser.getId()))
                .findFirst()
                .orElse(null);

        boolean isMember = myMembership != null;
        boolean isLeader = myMembership != null && myMembership.getRole() == MembershipRole.LEADER;
        boolean isManager = myMembership != null && myMembership.getRole() == MembershipRole.MANAGER;
        boolean isLeaderOrManager = isLeader || isManager;
        boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;

        // 멤버도 아니고 관리자도 아니면 접근 불가
        if (!isMember && !isAdmin) {
            return "redirect:/studies/" + studyId;
        }

        // 리더 → 매니저 → 멤버 순으로 정렬 (그 안에서는 사용자 ID 오름차순)
        memberships.sort(
                Comparator.comparing((Membership m) -> m.getRole() != MembershipRole.LEADER)
                        .thenComparing(m -> m.getRole() != MembershipRole.MANAGER)
                        .thenComparing(m -> m.getUser().getId()));

        prepareCommonModel(model, currentUser);

        model.addAttribute("study", study);
        model.addAttribute("memberships", memberships);
        model.addAttribute("isLeader", isLeader);
        model.addAttribute("isLeaderOrManager", isLeaderOrManager);
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("currentUserId", currentUser.getId());

        return "room/members";
    }

    /**
     * 멤버를 매니저로 승격 (리더 / 관리자 전용)
     */
    @PostMapping("/{membershipId}/promote")
    public String promoteToManager(@PathVariable("studyId") Long studyId,
            @PathVariable("membershipId") Long membershipId) {

        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        Study study = studyRepository.findById(studyId).orElse(null);
        if (study == null) {
            return "redirect:/studies";
        }

        Membership target = membershipRepository.findById(membershipId).orElse(null);
        if (target == null || !target.getStudy().getId().equals(studyId)) {
            return "redirect:/room/" + studyId + "/members";
        }

        List<Membership> memberships = membershipRepository.findByStudy(study);

        Membership myMembership = memberships.stream()
                .filter(m -> m.getUser().getId().equals(currentUser.getId()))
                .findFirst()
                .orElse(null);

        boolean isLeader = myMembership != null && myMembership.getRole() == MembershipRole.LEADER;
        boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;

        // 리더나 관리자만 역할 변경 가능
        if (!isLeader && !isAdmin) {
            return "redirect:/room/" + studyId + "/members";
        }

        // 리더는 승격 대상이 될 수 없음
        if (target.getRole() == MembershipRole.LEADER) {
            return "redirect:/room/" + studyId + "/members";
        }

        // 멤버만 매니저로 승격
        if (target.getRole() == MembershipRole.MEMBER) {
            target.setRole(MembershipRole.MANAGER);
            membershipRepository.save(target);
        }

        return "redirect:/room/" + studyId + "/members";
    }

    /**
     * 매니저를 일반 멤버로 강등 (리더 / 관리자 전용)
     */
    @PostMapping("/{membershipId}/demote")
    public String demoteToMember(@PathVariable("studyId") Long studyId,
            @PathVariable("membershipId") Long membershipId) {

        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        Study study = studyRepository.findById(studyId).orElse(null);
        if (study == null) {
            return "redirect:/studies";
        }

        Membership target = membershipRepository.findById(membershipId).orElse(null);
        if (target == null || !target.getStudy().getId().equals(studyId)) {
            return "redirect:/room/" + studyId + "/members";
        }

        List<Membership> memberships = membershipRepository.findByStudy(study);

        Membership myMembership = memberships.stream()
                .filter(m -> m.getUser().getId().equals(currentUser.getId()))
                .findFirst()
                .orElse(null);

        boolean isLeader = myMembership != null && myMembership.getRole() == MembershipRole.LEADER;
        boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;

        if (!isLeader && !isAdmin) {
            return "redirect:/room/" + studyId + "/members";
        }

        // 리더는 강등 대상이 될 수 없음
        if (target.getRole() == MembershipRole.LEADER) {
            return "redirect:/room/" + studyId + "/members";
        }

        // 매니저만 멤버로 강등
        if (target.getRole() == MembershipRole.MANAGER) {
            target.setRole(MembershipRole.MEMBER);
            membershipRepository.save(target);
        }

        return "redirect:/room/" + studyId + "/members";
    }

    /**
     * 멤버 강퇴 (리더/매니저/관리자)
     */
    @PostMapping("/{membershipId}/remove")
    public String removeMember(@PathVariable("studyId") Long studyId,
            @PathVariable("membershipId") Long membershipId) {

        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        Study study = studyRepository.findById(studyId).orElse(null);
        if (study == null) {
            return "redirect:/studies";
        }

        Membership target = membershipRepository.findById(membershipId).orElse(null);
        if (target == null || !target.getStudy().getId().equals(studyId)) {
            return "redirect:/room/" + studyId + "/members";
        }

        List<Membership> memberships = membershipRepository.findByStudy(study);

        Membership myMembership = memberships.stream()
                .filter(m -> m.getUser().getId().equals(currentUser.getId()))
                .findFirst()
                .orElse(null);

        boolean isMember = myMembership != null;
        boolean isLeader = isMember && myMembership.getRole() == MembershipRole.LEADER;
        boolean isManager = isMember && myMembership.getRole() == MembershipRole.MANAGER;
        boolean isLeaderOrManager = isLeader || isManager;
        boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;

        if (!isLeaderOrManager && !isAdmin) {
            return "redirect:/room/" + studyId + "/members";
        }

        // 리더는 누구도 강퇴할 수 없음 (리더가 없는 스터디가 되지 않도록 보호)
        if (target.getRole() == MembershipRole.LEADER) {
            return "redirect:/room/" + studyId + "/members";
        }

        // 매니저는 일반 멤버만 강퇴 가능, 자기 자신 강퇴 불가
        if (!isLeader && !isAdmin) { // 매니저인 경우
            if (target.getRole() != MembershipRole.MEMBER) {
                return "redirect:/room/" + studyId + "/members";
            }
            if (target.getUser().getId().equals(currentUser.getId())) {
                return "redirect:/room/" + studyId + "/members";
            }
        }

        // 리더/관리자는 리더를 제외한 모든 멤버 강퇴 가능 (자기 자신 포함 X)
        if ((isLeader || isAdmin) && target.getUser().getId().equals(currentUser.getId())) {
            // 이 화면에서는 자기 자신 강퇴는 허용하지 않음 (탈퇴 기능은 별도로 구현)
            return "redirect:/room/" + studyId + "/members";
        }

        membershipRepository.delete(target);

        return "redirect:/room/" + studyId + "/members";
    }

    /**
     * 스터디 탈퇴 (멤버 / 매니저용)
     */
    @PostMapping("/leave")
    public String leaveStudy(@PathVariable("studyId") Long studyId) {

        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        Study study = studyRepository.findById(studyId).orElse(null);
        if (study == null) {
            return "redirect:/studies";
        }

        List<Membership> memberships = membershipRepository.findByStudy(study);

        Membership myMembership = memberships.stream()
                .filter(m -> m.getUser().getId().equals(currentUser.getId()))
                .findFirst()
                .orElse(null);

        // 멤버가 아니면 탈퇴할 것도 없음
        if (myMembership == null) {
            return "redirect:/studies/" + studyId;
        }

        // 리더는 이 경로로 탈퇴 불가 (리더 교체/스터디 삭제는 별도 기능)
        if (myMembership.getRole() == MembershipRole.LEADER
                && currentUser.getRole() != UserRole.ADMIN) {
            return "redirect:/room/" + studyId + "/members";
        }

        membershipRepository.delete(myMembership);

        // 탈퇴 후에는 내 스터디 목록으로 이동
        return "redirect:/my/studies";
    }

    private void prepareCommonModel(Model model, User currentUser) {
        boolean loggedIn = currentUser != null;
        model.addAttribute("loggedIn", loggedIn);
        if (loggedIn) {
            model.addAttribute("loginEmail", currentUser.getEmail());
        }
    }
}
