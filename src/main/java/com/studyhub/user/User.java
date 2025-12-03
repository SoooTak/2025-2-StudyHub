package com.studyhub.user;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_email", columnNames = "email")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 로그인 ID로 쓸 이메일
    @Column(nullable = false, length = 100)
    private String email;

    // 나중에 BCrypt로 암호화 예정 (지금은 그냥 텍스트로 테스트)
    @Column(nullable = false, length = 255)
    private String password;

    // 실명
    @Column(nullable = false, length = 50)
    private String name;

    // 닉네임
    @Column(nullable = false, length = 50)
    private String nickname;

    // 전화번호 (선택)
    @Column(length = 20)
    private String phone;

    // 한 줄 소개 (선택)
    @Column(length = 255)
    private String intro;

    // 권한 (일반 유저 / 관리자)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    // 이메일 인증 여부
    @Column(nullable = false)
    private boolean emailVerified;

    // 생성/수정 시각
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;

        if (this.role == null) {
            this.role = UserRole.USER;
        }
        // 기본값: 아직 이메일 인증 안 됨
        // 필요하면 여기서 emailVerified 기본값 조정 가능
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
