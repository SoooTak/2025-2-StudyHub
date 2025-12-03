package com.studyhub.study;

import com.studyhub.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudyRepository extends JpaRepository<Study, Long> {

    // 공개 스터디 목록 (기존 /studies에서 사용)
    List<Study> findByIsPublicTrueOrderByCreatedAtDesc();

    // 내가 리더인 스터디 목록
    List<Study> findByLeader(User leader);
}
