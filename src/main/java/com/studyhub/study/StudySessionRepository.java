package com.studyhub.study;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudySessionRepository extends JpaRepository<StudySession, Long> {

    // 특정 스터디의 세션 목록 (시간 순)
    List<StudySession> findByStudyOrderBySessionDateTimeAsc(Study study);
}
