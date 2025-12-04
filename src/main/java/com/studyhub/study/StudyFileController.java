package com.studyhub.study;

import com.studyhub.user.User;
import com.studyhub.user.UserRole;
import com.studyhub.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriUtils;

import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/room/{studyId}/files")
public class StudyFileController {

    private final StudyRepository studyRepository;
    private final MembershipRepository membershipRepository;
    private final StudyFileService studyFileService;
    private final UserService userService;
    private final NotificationService notificationService;

    /**
     * 자료 목록 + 업로드 화면
     * GET /room/{studyId}/files
     */
    @GetMapping
    public String listFiles(@PathVariable("studyId") Long studyId, Model model) {

        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        Study study = studyRepository.findById(studyId).orElse(null);
        if (study == null) {
            return "redirect:/studies";
        }

        List<Membership> memberships = membershipRepository.findByStudy(study);

        boolean isMember = memberships.stream()
                .anyMatch(m -> m.getUser().getId().equals(currentUser.getId()));

        boolean isLeaderOrManager = memberships.stream()
                .anyMatch(m -> m.getUser().getId().equals(currentUser.getId())
                        && (m.getRole() == MembershipRole.LEADER
                                || m.getRole() == MembershipRole.MANAGER));

        boolean isLeader = memberships.stream()
                .anyMatch(m -> m.getUser().getId().equals(currentUser.getId())
                        && m.getRole() == MembershipRole.LEADER);

        boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;

        // 멤버도 아니고 관리자도 아니면 접근 불가
        if (!isMember && !isAdmin) {
            return "redirect:/studies/" + studyId;
        }

        List<StudyFile> files = studyFileService.getFilesForStudy(study);

        prepareCommonModel(model, currentUser);

        model.addAttribute("study", study);
        model.addAttribute("files", files);
        model.addAttribute("isMember", isMember);
        model.addAttribute("isLeader", isLeader);
        model.addAttribute("isLeaderOrManager", isLeaderOrManager);
        model.addAttribute("isAdmin", isAdmin);

        return "room/files";
    }

    /**
     * 파일 업로드 처리
     * POST /room/{studyId}/files/upload
     */
    @PostMapping("/upload")
    public String uploadFile(@PathVariable("studyId") Long studyId,
            @RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes) {

        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        Study study = studyRepository.findById(studyId).orElse(null);
        if (study == null) {
            return "redirect:/studies";
        }

        List<Membership> memberships = membershipRepository.findByStudy(study);

        boolean isMember = memberships.stream()
                .anyMatch(m -> m.getUser().getId().equals(currentUser.getId()));

        boolean isLeaderOrManager = memberships.stream()
                .anyMatch(m -> m.getUser().getId().equals(currentUser.getId())
                        && (m.getRole() == MembershipRole.LEADER
                                || m.getRole() == MembershipRole.MANAGER));

        boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;

        // 업로드는 리더/매니저/관리자만 가능
        if (!isLeaderOrManager && !isAdmin) {
            redirectAttributes.addFlashAttribute("errorMessage", "자료 업로드 권한이 없습니다.");
            return "redirect:/room/" + studyId + "/files";
        }

        try {
            // 실제 파일 저장
            studyFileService.storeFile(study, currentUser, file);

            // 업로드 성공 시, 스터디 멤버들에게 알림 생성
            notificationService.createStudyFileUploadedNotifications(
                    study,
                    currentUser,
                    file.getOriginalFilename());

            redirectAttributes.addFlashAttribute("message", "자료가 업로드되었습니다.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "파일 업로드 중 오류가 발생했습니다.");
        }

        return "redirect:/room/" + studyId + "/files";
    }

    /**
     * 파일 다운로드
     * GET /room/{studyId}/files/{fileId}/download
     */
    @GetMapping("/{fileId}/download")
    public ResponseEntity<Resource> downloadFile(@PathVariable("studyId") Long studyId,
            @PathVariable("fileId") Long fileId) {

        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            // 다운로드는 로그인 필요
            return ResponseEntity.status(302)
                    .header(HttpHeaders.LOCATION, "/login")
                    .build();
        }

        Study study = studyRepository.findById(studyId).orElse(null);
        if (study == null) {
            return ResponseEntity.status(302)
                    .header(HttpHeaders.LOCATION, "/studies")
                    .build();
        }

        List<Membership> memberships = membershipRepository.findByStudy(study);

        boolean isMember = memberships.stream()
                .anyMatch(m -> m.getUser().getId().equals(currentUser.getId()));

        boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;

        // 멤버도 아니고 관리자도 아니면 다운로드 불가
        if (!isMember && !isAdmin) {
            return ResponseEntity.status(302)
                    .header(HttpHeaders.LOCATION, "/studies/" + studyId)
                    .build();
        }

        StudyFile studyFile = studyFileService.findById(fileId).orElse(null);
        if (studyFile == null || !studyFile.getStudy().getId().equals(studyId)) {
            return ResponseEntity.notFound().build();
        }

        try {
            Resource resource = studyFileService.loadFileAsResource(studyFile);

            String contentType = studyFile.getContentType();
            if (contentType == null || contentType.isBlank()) {
                contentType = "application/octet-stream";
            }

            String encodedFileName = UriUtils.encode(studyFile.getOriginalFilename(), StandardCharsets.UTF_8);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + encodedFileName + "\"")
                    .body(resource);

        } catch (MalformedURLException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 파일 삭제
     * POST /room/{studyId}/files/{fileId}/delete
     */
    @PostMapping("/{fileId}/delete")
    public String deleteFile(@PathVariable("studyId") Long studyId,
            @PathVariable("fileId") Long fileId,
            RedirectAttributes redirectAttributes) {

        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        Study study = studyRepository.findById(studyId).orElse(null);
        if (study == null) {
            return "redirect:/studies";
        }

        List<Membership> memberships = membershipRepository.findByStudy(study);

        boolean isLeaderOrManager = memberships.stream()
                .anyMatch(m -> m.getUser().getId().equals(currentUser.getId())
                        && (m.getRole() == MembershipRole.LEADER
                                || m.getRole() == MembershipRole.MANAGER));

        boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;

        if (!isLeaderOrManager && !isAdmin) {
            redirectAttributes.addFlashAttribute("errorMessage", "자료 삭제 권한이 없습니다.");
            return "redirect:/room/" + studyId + "/files";
        }

        StudyFile studyFile = studyFileService.findById(fileId).orElse(null);
        if (studyFile == null || !studyFile.getStudy().getId().equals(studyId)) {
            redirectAttributes.addFlashAttribute("errorMessage", "자료를 찾을 수 없습니다.");
            return "redirect:/room/" + studyId + "/files";
        }

        studyFileService.deleteFile(studyFile);
        redirectAttributes.addFlashAttribute("message", "자료가 삭제되었습니다.");

        return "redirect:/room/" + studyId + "/files";
    }

    private void prepareCommonModel(Model model, User currentUser) {
        boolean loggedIn = currentUser != null;
        model.addAttribute("loggedIn", loggedIn);
        if (loggedIn) {
            model.addAttribute("loginEmail", currentUser.getEmail());
        }
    }
}
