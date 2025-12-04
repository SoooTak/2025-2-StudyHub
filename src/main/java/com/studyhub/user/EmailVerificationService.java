package com.studyhub.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationService {

    private final EmailVerificationRepository verificationRepository;
    private final UserRepository userRepository;

    /**
     * 회원가입 직후 이메일 인증 토큰 생성 + "발송"
     */
    public void createAndSendVerification(User user) {
        // 토큰 생성
        String token = UUID.randomUUID().toString().replace("-", "");

        EmailVerification verification = new EmailVerification();
        verification.setUser(user);
        verification.setToken(token);
        verification.setExpiresAt(LocalDateTime.now().plusHours(1)); // 1시간 유효

        verificationRepository.save(verification);

        // 실제로는 이메일 발송해야 함
        String verifyUrl = "http://localhost:8080/verify-email?token=" + token;
        log.info("[EMAIL-VERIFY] {} 에 인증 링크 발송: {}", user.getEmail(), verifyUrl);
    }

    /**
     * 토큰으로 이메일 인증 처리
     */
    public boolean verifyToken(String token) {
        EmailVerification verification = verificationRepository.findByTokenAndUsedFalse(token)
                .orElse(null);

        if (verification == null) {
            return false;
        }
        if (verification.isExpired()) {
            return false;
        }

        User user = verification.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        verification.setUsed(true);
        verificationRepository.save(verification);

        return true;
    }
}
