package com.studyhub.home;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("title", "StudyHub 메인");
        model.addAttribute("message", "스터디 통합 운영 웹사이트 - StudyHub 시작 페이지입니다.");
        return "index";
    }
}
