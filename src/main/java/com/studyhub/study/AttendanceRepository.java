package com.studyhub.study;

import com.studyhub.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    // 특정 세션 + 특정 유저 출석 기록
    Optional<Attendance> findBySessionAndUser(StudySession session, User user);

    // 특정 세션의 전체 출석 목록 (나중에 리더가 출석부 확인용으로 쓸 수 있음)
    List<Attendance> findBySession(StudySession session);
}
