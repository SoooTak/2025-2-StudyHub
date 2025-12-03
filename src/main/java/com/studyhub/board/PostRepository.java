package com.studyhub.board;

import com.studyhub.study.Study;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    // 해당 스터디의 전체 게시글 (삭제되지 않은 것만), 최신순
    List<Post> findByStudyAndDeletedFalseOrderByCreatedAtDesc(Study study);

    // 해당 스터디의 특정 타입(공지/일반) 게시글, 최신순
    List<Post> findByStudyAndTypeAndDeletedFalseOrderByCreatedAtDesc(Study study, PostType type);
}
