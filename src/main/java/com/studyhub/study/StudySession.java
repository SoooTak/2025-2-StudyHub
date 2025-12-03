package com.studyhub.study;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "study_session")
public class StudySession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어떤 스터디의 세션인지
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "study_id")
    private Study study;

    // 세션 제목 (예: 1주차 OT, 알고리즘 스터디 1회차)
    @Column(nullable = false, length = 100)
    private String title;

    // 진행 일시
    @Column(name = "session_datetime", nullable = false)
    private LocalDateTime sessionDateTime;

    // 장소 (온라인이라면 "온라인", "Zoom" 등 자유 입력)
    @Column(length = 200)
    private String location;

    public Long getId() {
        return id;
    }

    public Study getStudy() {
        return study;
    }

    public void setStudy(Study study) {
        this.study = study;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDateTime getSessionDateTime() {
        return sessionDateTime;
    }

    public void setSessionDateTime(LocalDateTime sessionDateTime) {
        this.sessionDateTime = sessionDateTime;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}
