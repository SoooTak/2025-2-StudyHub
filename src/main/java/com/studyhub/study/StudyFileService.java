package com.studyhub.study;

import com.studyhub.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StudyFileService {

    private final StudyFileRepository studyFileRepository;

    // 파일이 실제로 저장될 베이스 디렉터리 (디폴트: 프로젝트 루트 기준 ./uploads/study-files)
    @Value("${studyhub.upload-dir:uploads/study-files}")
    private String uploadBaseDir;

    // 허용 확장자 목록
    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>();

    static {
        ALLOWED_EXTENSIONS.add("pdf");
        ALLOWED_EXTENSIONS.add("ppt");
        ALLOWED_EXTENSIONS.add("pptx");
        ALLOWED_EXTENSIONS.add("doc");
        ALLOWED_EXTENSIONS.add("docx");
        ALLOWED_EXTENSIONS.add("xls");
        ALLOWED_EXTENSIONS.add("xlsx");
        ALLOWED_EXTENSIONS.add("zip");
        ALLOWED_EXTENSIONS.add("png");
        ALLOWED_EXTENSIONS.add("jpg");
        ALLOWED_EXTENSIONS.add("jpeg");
    }

    // 스터디 자료 목록
    public List<StudyFile> getFilesForStudy(Study study) {
        return studyFileRepository.findByStudyOrderByUploadedAtDesc(study);
    }

    public Optional<StudyFile> findById(Long id) {
        return studyFileRepository.findById(id);
    }

    // 파일 저장
    public StudyFile storeFile(Study study, User uploader, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 파일을 선택해 주세요.");
        }

        // 50MB 제한
        long maxSize = 50L * 1024L * 1024L;
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("최대 50MB까지 업로드할 수 있습니다.");
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        if (!isAllowedExtension(originalFilename)) {
            throw new IllegalArgumentException("허용되지 않는 파일 형식입니다.");
        }

        // 스터디당 최대 100개까지
        long count = studyFileRepository.countByStudy(study);
        if (count >= 100) {
            throw new IllegalStateException("이 스터디에는 이미 100개의 자료가 등록되어 있습니다.");
        }

        // 디렉터리 준비: {uploadBaseDir}/study-{studyId}/
        Path basePath = Paths.get(uploadBaseDir).toAbsolutePath().normalize();
        Path studyDir = basePath.resolve("study-" + study.getId());
        Files.createDirectories(studyDir);

        // 파일 이름: uuid + 확장자
        String ext = getExtension(originalFilename);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String storedFileNameOnly = uuid + (ext.isEmpty() ? "" : "." + ext);

        Path targetPath = studyDir.resolve(storedFileNameOnly);

        // 파일 실제 저장
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }

        // DB에 저장할 상대 경로: "study-{id}/파일이름"
        String storedRelativePath = "study-" + study.getId() + "/" + storedFileNameOnly;

        StudyFile studyFile = StudyFile.builder()
                .study(study)
                .uploader(uploader)
                .originalFilename(originalFilename)
                .storedFilename(storedRelativePath)
                .contentType(file.getContentType())
                .size(file.getSize())
                .uploadedAt(LocalDateTime.now())
                .build();

        return studyFileRepository.save(studyFile);
    }

    // 파일 삭제 (실제 파일 + DB)
    public void deleteFile(StudyFile studyFile) {
        // 실제 파일 삭제
        if (studyFile.getStoredFilename() != null && !studyFile.getStoredFilename().isEmpty()) {
            Path basePath = Paths.get(uploadBaseDir).toAbsolutePath().normalize();
            Path filePath = basePath.resolve(studyFile.getStoredFilename()).normalize();
            try {
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                // 실제 파일이 없어도, DB에서만 삭제되면 사용에는 지장 없으니 로그만 남기고 무시해도 됨
                // 여기서는 단순히 무시
            }
        }

        // DB에서 삭제
        studyFileRepository.delete(studyFile);
    }

    // 다운로드용 Resource 로드
    public Resource loadFileAsResource(StudyFile studyFile) throws MalformedURLException {
        Path basePath = Paths.get(uploadBaseDir).toAbsolutePath().normalize();
        Path filePath = basePath.resolve(studyFile.getStoredFilename()).normalize();
        Resource resource = new UrlResource(filePath.toUri());
        if (resource.exists() && resource.isReadable()) {
            return resource;
        }
        throw new MalformedURLException("파일을 찾을 수 없습니다: " + studyFile.getStoredFilename());
    }

    private boolean isAllowedExtension(String filename) {
        String ext = getExtension(filename);
        if (ext.isEmpty()) {
            return false;
        }
        return ALLOWED_EXTENSIONS.contains(ext.toLowerCase());
    }

    private String getExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int dotIdx = filename.lastIndexOf('.');
        if (dotIdx == -1 || dotIdx == filename.length() - 1) {
            return "";
        }
        return filename.substring(dotIdx + 1);
    }
}
