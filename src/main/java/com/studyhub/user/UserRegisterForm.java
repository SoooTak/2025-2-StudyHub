package com.studyhub.user;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserRegisterForm {

    // 로그인 이메일
    private String email;

    // 비밀번호
    private String password;

    // 비밀번호 확인
    private String confirmPassword;

    // 실명
    private String name;

    // 닉네임
    private String nickname;

    // 전화번호 (선택)
    private String phone;

    // 한 줄 소개 (선택)
    private String intro;
}
