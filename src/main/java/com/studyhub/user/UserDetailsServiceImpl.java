package com.studyhub.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + email));

        // 이메일 미인증이면 로그인 거부
        // (User에 boolean emailVerified 필드 있다고 가정)
        if (!user.isEmailVerified()) {
            throw new UsernameNotFoundException("이메일 인증이 완료되지 않았습니다.");
        }

        String roleName = user.getRole().name(); // USER 또는 ADMIN

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .roles(roleName) // "ROLE_USER" 또는 "ROLE_ADMIN"
                .build();
    }
}
