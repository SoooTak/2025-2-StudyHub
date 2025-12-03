package com.studyhub.study;

import com.studyhub.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "studies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Study {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 스터디 이름
    @Column(nullable = false, length = 100)
    private String title;

    // 간단 소개
    @Column(nullable = false, length = 1000)
    private String description;

    // 카테고리 (예: 프로그래밍, 어학…)
    @Column(length = 50)
    private String category;

    // 온/오프라인 정보 (예: 온라인, 오프라인, 혼합)
    @Column(length = 50)
    private String studyMode;

    // 오프라인 장소 (선택)
    @Column(length = 255)
    private String location;

    // 최대 인원 (리더 포함)
    @Column(nullable = false)
    private Integer maxMembers;

    // 공개 여부
    @Column(nullable = false)
    private boolean isPublic;

    // 리더
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leader_id", nullable = false)
    private User leader;

    // 생성일시
    @Column(nullable = false)
    private LocalDateTime createdAt;

    // 수정일시
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
