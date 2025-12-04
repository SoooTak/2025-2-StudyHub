package com.studyhub.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 비밀번호 재설정 요청
     */
    public void requestReset(String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            // 굳이 에러 안 내고 그냥 통과 (보안상 보통 이렇게 함)
            log.warn("[PW-RESET] 존재하지 않는 이메일로 요청: {}", email);
            return;
        }

        String token = UUID.randomUUID().toString().replace("-", "");

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setToken(token);
        resetToken.setExpiresAt(LocalDateTime.now().plusHours(1));

        tokenRepository.save(resetToken);

        String resetUrl = "http://localhost:8080/reset-password?token=" + token;
        log.info("[PW-RESET] {} 에 비밀번호 재설정 링크 발송: {}", email, resetUrl);
    }

    /**
     * 토큰 유효성 검증
     */
    public PasswordResetToken validateToken(String token) {
        PasswordResetToken resetToken = tokenRepository.findByTokenAndUsedFalse(token).orElse(null);
        if (resetToken == null || resetToken.isExpired()) {
            return null;
        }
        return resetToken;
    }

    /**
     * 실제 비밀번호 변경
     */
    public boolean resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = validateToken(token);
        if (resetToken == null) {
            return false;
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);

        return true;
    }
}
