package com.studyhub.study;

import com.studyhub.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "study_files")
public class StudyFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어떤 스터디의 자료인지
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "study_id")
    private Study study;

    // 누가 올렸는지
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploader_id")
    private User uploader;

    // 사용자가 올린 원래 파일 이름
    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    // 서버에 저장된 파일 경로(업로드 베이스 디렉터리 기준 상대 경로)
    @Column(name = "stored_filename", nullable = false, length = 255)
    private String storedFilename;

    // Content-Type (예: application/pdf)
    @Column(name = "content_type", length = 255)
    private String contentType;

    // 파일 크기 (바이트)
    @Column(name = "size", nullable = false)
    private long size;

    // 업로드 시각
    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;
}
