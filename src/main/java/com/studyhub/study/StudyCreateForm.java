package com.studyhub.study;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StudyCreateForm {

    private String title;
    private String description;
    private String category;
    private String studyMode;
    private String location;
    private Integer maxMembers;
    private boolean isPublic = true;
}
