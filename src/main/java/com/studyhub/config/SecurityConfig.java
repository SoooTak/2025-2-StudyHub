package com.studyhub.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsService userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        // 회원가입 / 로그인 둘 다 이 인코더를 사용
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                // 개발 편의를 위해 CSRF 잠깐 비활성화 (실서비스에서는 켜야 함)
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth
                        // ✅ 누구나 접근 가능 (이메일/비번 관련 포함)
                        .requestMatchers(
                                "/", // 메인
                                "/signup", // 회원가입
                                "/login", // 로그인 폼
                                "/verify-email", // 이메일 인증
                                "/forgot-password", // 비밀번호 재설정 요청
                                "/reset-password", // 비밀번호 재설정 처리
                                "/css/**", "/js/**", "/images/**")
                        .permitAll()

                        // ✅ 공개 스터디 목록/상세는 로그인 없이도 볼 수 있게
                        .requestMatchers("/studies", "/studies/**").permitAll()

                        // ✅ 관리자 전용
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // 그 외 나머지는 전부 로그인 필요
                        .anyRequest().authenticated())

                // 우리가 만드는 UserDetailsService 사용
                .userDetailsService(userDetailsService)

                // 폼 로그인 설정
                .formLogin(login -> login
                        .loginPage("/login") // GET /login 페이지
                        .defaultSuccessUrl("/", false) // 저장된 요청 있으면 그쪽, 없으면 /
                        .usernameParameter("email") // 로그인 폼의 name="email"
                        .passwordParameter("password") // 로그인 폼의 name="password"
                        .permitAll())

                // 로그아웃 설정
                .logout(logout -> logout
                        .logoutUrl("/logout") // POST /logout
                        .logoutSuccessUrl("/") // 로그아웃 후 /
                        .permitAll());

        return http.build();
    }
}
