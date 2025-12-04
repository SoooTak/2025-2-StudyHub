package com.studyhub.study;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttendanceChangeLogRepository extends JpaRepository<AttendanceChangeLog, Long> {
    List<AttendanceChangeLog> findBySessionOrderByChangedAtDesc(StudySession session);
}
