package com.studyhub.study;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudyFileRepository extends JpaRepository<StudyFile, Long> {

    // 특정 스터디의 자료 목록 (최신 순)
    List<StudyFile> findByStudyOrderByUploadedAtDesc(Study study);

    // 특정 스터디의 자료 개수
    long countByStudy(Study study);
}
