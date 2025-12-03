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
                        // 누구나 접근 가능
                        .requestMatchers("/", "/signup", "/login", "/css/**", "/js/**", "/images/**").permitAll()
                        .requestMatchers("/studies", "/studies/**").permitAll()
                        // 나머지는 인증 필요
                        .anyRequest().authenticated())

                // 우리가 만드는 UserDetailsService 사용
                .userDetailsService(userDetailsService)

                // 폼 로그인 설정
                .formLogin(login -> login
                        .loginPage("/login") // GET /login 페이지
                        // 저장된 요청이 있으면 그쪽으로, 없으면 / 로
                        .defaultSuccessUrl("/", false)
                        .usernameParameter("email") // 폼에서 이메일 input name
                        .passwordParameter("password")// 폼에서 비밀번호 input name
                        .permitAll())

                // 로그아웃 설정
                .logout(logout -> logout
                        .logoutUrl("/logout") // POST /logout
                        .logoutSuccessUrl("/") // 로그아웃 후 /
                        .permitAll());

        return http.build();
    }
}
