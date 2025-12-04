package com.studyhub.study;

import com.studyhub.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserOrderByCreatedAtDesc(User user);

    List<Notification> findByUserAndReadFalseOrderByCreatedAtDesc(User user);
    
    List<Membership> findByStudy(Study study);

    Optional<Notification> findByIdAndUser(Long id, User user);

    /**
     * 읽지 않은 알림 개수
     */
    long countByUserAndReadFalse(User user);
}
