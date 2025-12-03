package com.studyhub.study;

import com.studyhub.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    // 특정 스터디 + 특정 사용자 + 상태로 한 건 찾기 (이미 PENDING 신청 있는지 확인할 때 사용)
    Optional<Application> findByStudyAndApplicantAndStatus(Study study, User applicant, ApplicationStatus status);

    // 특정 스터디의 특정 상태(PENDING 등) 신청 목록 (최근 ID 순)
    List<Application> findByStudyAndStatusOrderByIdDesc(Study study, ApplicationStatus status);

    // 특정 스터디의 전체 신청 목록 (최근 ID 순) - 필요하면 사용
    List<Application> findByStudyOrderByIdDesc(Study study);
}
