package com.studyhub.board;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    // 글에 달린 댓글 목록 (삭제되지 않은 것만), 오래된 순
    List<Comment> findByPostAndDeletedFalseOrderByCreatedAtAsc(Post post);
}
