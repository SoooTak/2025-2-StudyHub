package com.studyhub.study;

import com.studyhub.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final AttendanceChangeLogRepository logRepository;

    @Transactional
    public void updateAttendance(StudySession session, User targetUser, AttendanceStatus newStatus, User changedBy) {
        Attendance attendance = attendanceRepository.findBySessionAndUser(session, targetUser)
                .orElseThrow(() -> new IllegalArgumentException("출석 기록이 없습니다."));

        AttendanceStatus oldStatus = attendance.getStatus();
        if (oldStatus == newStatus)
            return;

        attendance.setStatus(newStatus);
        attendance.setCheckedAt(LocalDateTime.now());
        attendanceRepository.save(attendance);

        AttendanceChangeLog log = AttendanceChangeLog.builder()
                .session(session)
                .targetUser(targetUser)
                .changedBy(changedBy)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .changedAt(LocalDateTime.now())
                .build();

        logRepository.save(log);
    }
}
