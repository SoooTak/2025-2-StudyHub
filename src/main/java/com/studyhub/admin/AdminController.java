package com.studyhub.admin;

import com.studyhub.study.Study;
import com.studyhub.study.StudyRepository;
import com.studyhub.user.User;
import com.studyhub.user.UserRepository;
import com.studyhub.user.UserRole;
import com.studyhub.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final StudyRepository studyRepository;

    /**
     * /admin -> /admin/dashboard
     */
    @GetMapping
    public String adminRoot() {
        return "redirect:/admin/dashboard";
    }

    /**
     * 관리자 권한 체크
     */
    private boolean isAdmin(User currentUser) {
        return currentUser != null && currentUser.getRole() == UserRole.ADMIN;
    }

    /**
     * 간단 대시보드: 유저 수 / 스터디 수
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        User currentUser = userService.getCurrentUser();
        if (!isAdmin(currentUser)) {
            return "redirect:/";
        }

        long userCount = userRepository.count();
        long studyCount = studyRepository.count();

        model.addAttribute("userCount", userCount);
        model.addAttribute("studyCount", studyCount);

        return "admin/dashboard";
    }

    /**
     * 유저 목록
     */
    @GetMapping("/users")
    public String listUsers(Model model) {
        User currentUser = userService.getCurrentUser();
        if (!isAdmin(currentUser)) {
            return "redirect:/";
        }

        List<User> users = userRepository.findAll();
        model.addAttribute("users", users);
        return "admin/users";
    }

    /**
     * 유저 역할 변경 (USER <-> ADMIN)
     */
    @PostMapping("/users/{id}/role")
    public String changeUserRole(@PathVariable("id") Long id,
            @RequestParam("role") String role) {

        User currentUser = userService.getCurrentUser();
        if (!isAdmin(currentUser)) {
            return "redirect:/";
        }

        User target = userRepository.findById(id).orElse(null);
        if (target != null) {
            if ("ADMIN".equalsIgnoreCase(role)) {
                target.setRole(UserRole.ADMIN);
            } else {
                target.setRole(UserRole.USER);
            }
            userRepository.save(target);
        }

        return "redirect:/admin/users";
    }

    /**
     * 스터디 목록
     */
    @GetMapping("/studies")
    public String listStudies(Model model) {
        User currentUser = userService.getCurrentUser();
        if (!isAdmin(currentUser)) {
            return "redirect:/";
        }

        List<Study> studies = studyRepository.findAll();
        model.addAttribute("studies", studies);
        return "admin/studies";
    }

    /**
     * 스터디 숨기기 (공개 -> 비공개)
     */
    @PostMapping("/studies/{id}/hide")
    public String hideStudy(@PathVariable("id") Long id) {
        User currentUser = userService.getCurrentUser();
        if (!isAdmin(currentUser)) {
            return "redirect:/";
        }

        Study study = studyRepository.findById(id).orElse(null);
        if (study != null) {
            study.setPublic(false);
            studyRepository.save(study);
        }

        return "redirect:/admin/studies";
    }

    /**
     * 스터디 다시 보이게 (비공개 -> 공개)
     */
    @PostMapping("/studies/{id}/show")
    public String showStudy(@PathVariable("id") Long id) {
        User currentUser = userService.getCurrentUser();
        if (!isAdmin(currentUser)) {
            return "redirect:/";
        }

        Study study = studyRepository.findById(id).orElse(null);
        if (study != null) {
            study.setPublic(true);
            studyRepository.save(study);
        }

        return "redirect:/admin/studies";
    }
}
