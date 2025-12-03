package com.studyhub.study;

import com.studyhub.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MembershipRepository extends JpaRepository<Membership, Long> {

    // 이 스터디에 이 유저가 멤버로 있는지 여부
    boolean existsByStudyAndUser(Study study, User user);

    // 특정 스터디의 모든 멤버십 목록 (리더 출석부 등에서 사용)
    List<Membership> findByStudy(Study study);

    // 특정 스터디 + 특정 유저의 멤버십 1건
    Optional<Membership> findByStudyAndUser(Study study, User user);

    // 특정 유저가 MEMBER로 참여 중인 스터디들 (내가 속한 스터디 목록 등에서 사용)
    List<Membership> findByUserAndRole(User user, MembershipRole role);
}
