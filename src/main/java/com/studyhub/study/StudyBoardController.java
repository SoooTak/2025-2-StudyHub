package com.studyhub.study;

import com.studyhub.board.Comment;
import com.studyhub.board.CommentForm;
import com.studyhub.board.CommentRepository;
import com.studyhub.board.Post;
import com.studyhub.board.PostForm;
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
@RequiredArgsConstructor
@RequestMapping("/room/{studyId}/board")
public class StudyBoardController {

    private final StudyRepository studyRepository;
    private final MembershipRepository membershipRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserService userService;

    // 게시판 목록 + 글쓰기 폼
    @GetMapping
    public String list(@PathVariable("studyId") Long studyId, Model model) {

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
                        && (m.getRole() == MembershipRole.LEADER || m.getRole() == MembershipRole.MANAGER));

        boolean isLeader = memberships.stream()
                .anyMatch(m -> m.getUser().getId().equals(currentUser.getId())
                        && m.getRole() == MembershipRole.LEADER);

        boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;

        // 멤버도 아니고 관리자도 아니면 접근 불가
        if (!isMember && !isAdmin) {
            return "redirect:/studies/" + studyId;
        }

        // 공지 + 일반 글 목록
        List<Post> noticePosts = postRepository.findByStudyAndTypeAndDeletedFalseOrderByCreatedAtDesc(study,
                PostType.NOTICE);
        List<Post> posts = postRepository.findByStudyAndTypeAndDeletedFalseOrderByCreatedAtDesc(study, PostType.NORMAL);

        prepareCommonModel(model, currentUser);

        model.addAttribute("study", study);
        model.addAttribute("isLeader", isLeader);
        model.addAttribute("isLeaderOrManager", isLeaderOrManager);
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("noticePosts", noticePosts);
        model.addAttribute("posts", posts);
        model.addAttribute("postForm", new PostForm());

        return "room/board";
    }

    // 새 글 작성
    @PostMapping("/new")
    public String create(@PathVariable("studyId") Long studyId,
            @ModelAttribute("postForm") PostForm form) {

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
                        && (m.getRole() == MembershipRole.LEADER || m.getRole() == MembershipRole.MANAGER));

        boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;

        if (!isMember && !isAdmin) {
            return "redirect:/studies/" + studyId;
        }

        if (form.getTitle() == null || form.getTitle().isBlank()
                || form.getContent() == null || form.getContent().isBlank()) {
            return "redirect:/room/" + studyId + "/board";
        }

        PostType type = form.getType();
        if (type == null) {
            type = PostType.NORMAL;
        }
        // 공지 = 리더/매니저/관리자만 허용, 아니면 NORMAL로
        if (type == PostType.NOTICE && !(isLeaderOrManager || isAdmin)) {
            type = PostType.NORMAL;
        }

        Post post = new Post();
        post.setStudy(study);
        post.setWriter(currentUser);
        post.setTitle(form.getTitle());
        post.setContent(form.getContent());
        post.setType(type);

        postRepository.save(post);

        return "redirect:/room/" + studyId + "/board";
    }

    // 글 상세 + 댓글 목록
    @GetMapping("/{postId}")
    public String detail(@PathVariable("studyId") Long studyId,
            @PathVariable("postId") Long postId,
            Model model) {

        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        Study study = studyRepository.findById(studyId).orElse(null);
        if (study == null) {
            return "redirect:/studies";
        }

        Post post = postRepository.findById(postId).orElse(null);
        if (post == null || post.isDeleted() || !post.getStudy().getId().equals(studyId)) {
            return "redirect:/room/" + studyId + "/board";
        }

        List<Membership> memberships = membershipRepository.findByStudy(study);

        boolean isMember = memberships.stream()
                .anyMatch(m -> m.getUser().getId().equals(currentUser.getId()));

        boolean isLeaderOrManager = memberships.stream()
                .anyMatch(m -> m.getUser().getId().equals(currentUser.getId())
                        && (m.getRole() == MembershipRole.LEADER || m.getRole() == MembershipRole.MANAGER));

        boolean isLeader = memberships.stream()
                .anyMatch(m -> m.getUser().getId().equals(currentUser.getId())
                        && m.getRole() == MembershipRole.LEADER);

        boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;

        if (!isMember && !isAdmin) {
            return "redirect:/studies/" + studyId;
        }

        boolean isOwner = post.getWriter().getId().equals(currentUser.getId());
        boolean canDelete = isOwner || isLeaderOrManager || isAdmin;

        List<Comment> comments = commentRepository.findByPostAndDeletedFalseOrderByCreatedAtAsc(post);

        prepareCommonModel(model, currentUser);

        model.addAttribute("study", study);
        model.addAttribute("post", post);
        model.addAttribute("comments", comments);
        model.addAttribute("isLeader", isLeader);
        model.addAttribute("isLeaderOrManager", isLeaderOrManager);
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("isOwner", isOwner);
        model.addAttribute("canDelete", canDelete);
        model.addAttribute("currentUserId", currentUser.getId());
        model.addAttribute("commentForm", new CommentForm());

        return "room/board_detail";
    }

    // 글 삭제 (소프트 삭제)
    @PostMapping("/{postId}/delete")
    public String delete(@PathVariable("studyId") Long studyId,
            @PathVariable("postId") Long postId) {

        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        Study study = studyRepository.findById(studyId).orElse(null);
        if (study == null) {
            return "redirect:/studies";
        }

        Post post = postRepository.findById(postId).orElse(null);
        if (post == null || post.isDeleted() || !post.getStudy().getId().equals(studyId)) {
            return "redirect:/room/" + studyId + "/board";
        }

        List<Membership> memberships = membershipRepository.findByStudy(study);

        boolean isLeaderOrManager = memberships.stream()
                .anyMatch(m -> m.getUser().getId().equals(currentUser.getId())
                        && (m.getRole() == MembershipRole.LEADER || m.getRole() == MembershipRole.MANAGER));
        boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;
        boolean isOwner = post.getWriter().getId().equals(currentUser.getId());

        boolean canDelete = isOwner || isLeaderOrManager || isAdmin;

        if (!canDelete) {
            return "redirect:/room/" + studyId + "/board/" + postId;
        }

        post.setDeleted(true);
        post.setDeletedAt(LocalDateTime.now());
        postRepository.save(post);

        return "redirect:/room/" + studyId + "/board";
    }

    // 글 수정 폼
    @GetMapping("/{postId}/edit")
    public String editForm(@PathVariable("studyId") Long studyId,
            @PathVariable("postId") Long postId,
            Model model) {

        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        Study study = studyRepository.findById(studyId).orElse(null);
        if (study == null) {
            return "redirect:/studies";
        }

        Post post = postRepository.findById(postId).orElse(null);
        if (post == null || post.isDeleted() || !post.getStudy().getId().equals(studyId)) {
            return "redirect:/room/" + studyId + "/board";
        }

        // 자기 글만 수정 가능
        if (!post.getWriter().getId().equals(currentUser.getId())) {
            return "redirect:/room/" + studyId + "/board/" + postId;
        }

        PostForm form = new PostForm();
        form.setTitle(post.getTitle());
        form.setContent(post.getContent());
        form.setType(post.getType());

        prepareCommonModel(model, currentUser);

        model.addAttribute("study", study);
        model.addAttribute("post", post);
        model.addAttribute("postForm", form);

        return "room/board_edit";
    }

    // 글 수정 처리
    @PostMapping("/{postId}/edit")
    public String edit(@PathVariable("studyId") Long studyId,
            @PathVariable("postId") Long postId,
            @ModelAttribute("postForm") PostForm form) {

        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        Study study = studyRepository.findById(studyId).orElse(null);
        if (study == null) {
            return "redirect:/studies";
        }

        Post post = postRepository.findById(postId).orElse(null);
        if (post == null || post.isDeleted() || !post.getStudy().getId().equals(studyId)) {
            return "redirect:/room/" + studyId + "/board";
        }

        // 자기 글만 수정 가능
        if (!post.getWriter().getId().equals(currentUser.getId())) {
            return "redirect:/room/" + studyId + "/board/" + postId;
        }

        if (form.getTitle() == null || form.getTitle().isBlank()
                || form.getContent() == null || form.getContent().isBlank()) {
            return "redirect:/room/" + studyId + "/board/" + postId + "/edit";
        }

        post.setTitle(form.getTitle());
        post.setContent(form.getContent());
        // 타입은 여기서 변경하지 않음
        postRepository.save(post);

        return "redirect:/room/" + studyId + "/board/" + postId;
    }

    // 댓글 작성
    @PostMapping("/{postId}/comments")
    public String addComment(@PathVariable("studyId") Long studyId,
            @PathVariable("postId") Long postId,
            @ModelAttribute("commentForm") CommentForm form) {

        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        Study study = studyRepository.findById(studyId).orElse(null);
        if (study == null) {
            return "redirect:/studies";
        }

        Post post = postRepository.findById(postId).orElse(null);
        if (post == null || post.isDeleted() || !post.getStudy().getId().equals(studyId)) {
            return "redirect:/room/" + studyId + "/board";
        }

        List<Membership> memberships = membershipRepository.findByStudy(study);

        boolean isMember = memberships.stream()
                .anyMatch(m -> m.getUser().getId().equals(currentUser.getId()));
        boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;

        if (!isMember && !isAdmin) {
            return "redirect:/studies/" + studyId;
        }

        if (form.getContent() == null || form.getContent().isBlank()) {
            return "redirect:/room/" + studyId + "/board/" + postId;
        }

        Comment comment = new Comment();
        comment.setPost(post);
        comment.setWriter(currentUser);
        comment.setContent(form.getContent());

        commentRepository.save(comment);

        return "redirect:/room/" + studyId + "/board/" + postId;
    }

    // 댓글 수정 처리 (자기 댓글만, 같은 페이지에서)
    @PostMapping("/{postId}/comments/{commentId}/edit")
    public String editComment(@PathVariable("studyId") Long studyId,
            @PathVariable("postId") Long postId,
            @PathVariable("commentId") Long commentId,
            @ModelAttribute("commentForm") CommentForm form) {

        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        Study study = studyRepository.findById(studyId).orElse(null);
        if (study == null) {
            return "redirect:/studies";
        }

        Post post = postRepository.findById(postId).orElse(null);
        if (post == null || post.isDeleted() || !post.getStudy().getId().equals(studyId)) {
            return "redirect:/room/" + studyId + "/board";
        }

        Comment comment = commentRepository.findById(commentId).orElse(null);
        if (comment == null || comment.isDeleted() || !comment.getPost().getId().equals(postId)) {
            return "redirect:/room/" + studyId + "/board/" + postId;
        }

        // 자기 댓글만 수정 가능
        if (!comment.getWriter().getId().equals(currentUser.getId())) {
            return "redirect:/room/" + studyId + "/board/" + postId;
        }

        if (form.getContent() == null || form.getContent().isBlank()) {
            return "redirect:/room/" + studyId + "/board/" + postId;
        }

        comment.setContent(form.getContent());
        commentRepository.save(comment);

        return "redirect:/room/" + studyId + "/board/" + postId;
    }

    // 댓글 삭제 (소프트 삭제)
    @PostMapping("/{postId}/comments/{commentId}/delete")
    public String deleteComment(@PathVariable("studyId") Long studyId,
            @PathVariable("postId") Long postId,
            @PathVariable("commentId") Long commentId) {

        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        Study study = studyRepository.findById(studyId).orElse(null);
        if (study == null) {
            return "redirect:/studies";
        }

        Post post = postRepository.findById(postId).orElse(null);
        if (post == null || post.isDeleted() || !post.getStudy().getId().equals(studyId)) {
            return "redirect:/room/" + studyId + "/board";
        }

        Comment comment = commentRepository.findById(commentId).orElse(null);
        if (comment == null || comment.isDeleted() || !comment.getPost().getId().equals(postId)) {
            return "redirect:/room/" + studyId + "/board/" + postId;
        }

        List<Membership> memberships = membershipRepository.findByStudy(study);

        boolean isLeaderOrManager = memberships.stream()
                .anyMatch(m -> m.getUser().getId().equals(currentUser.getId())
                        && (m.getRole() == MembershipRole.LEADER || m.getRole() == MembershipRole.MANAGER));
        boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;
        boolean isOwner = comment.getWriter().getId().equals(currentUser.getId());

        boolean canDelete = isOwner || isLeaderOrManager || isAdmin;

        if (!canDelete) {
            return "redirect:/room/" + studyId + "/board/" + postId;
        }

        comment.setDeleted(true);
        comment.setDeletedAt(LocalDateTime.now());
        commentRepository.save(comment);

        return "redirect:/room/" + studyId + "/board/" + postId;
    }

    private void prepareCommonModel(Model model, User currentUser) {
        boolean loggedIn = currentUser != null;
        model.addAttribute("loggedIn", loggedIn);
        if (loggedIn) {
            model.addAttribute("loginEmail", currentUser.getEmail());
        }
    }
}
