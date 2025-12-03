package com.studyhub.study;

import lombok.Getter;
import lombok.Setter;

/**
 * 스터디 개설/수정 폼에서 사용하는 DTO
 * (검증 라이브러리는 쓰지 않고, 컨트롤러에서 간단히 체크만 함)
 */
@Getter
@Setter
public class StudyForm {

    private Long id;

    // 스터디 제목
    private String title;

    // 스터디 소개/설명
    private String description;

    // 카테고리 (예: 알고리즘, 토익, CS, 자격증 등)
    private String category;

    // 최대 인원 수
    private Integer maxMembers;

    // 공개 여부 (true: 공개, false: 비공개)
    private boolean open = true;
}
