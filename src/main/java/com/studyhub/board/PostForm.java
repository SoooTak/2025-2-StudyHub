package com.studyhub.board;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PostForm {

    private String title;
    private String content;
    private PostType type; // NOTICE 또는 NORMAL
}
