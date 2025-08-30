package com.example.todo.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Service
public class VerifyCodeService {

    private final Duration codeTtl = Duration.ofMinutes(5);
    private final String prefix = "register:code:";
    private final String prefix2 = "reset_pwd:code:";

    private final StringRedisTemplate redisTemplate;

    public VerifyCodeService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String generateCode() { return String.format("%06d", new SecureRandom().nextInt(1000000)); }

    public void saveCode(String email, String code) {
        redisTemplate.opsForValue().set(prefix + email, code, codeTtl);
    }

    public void saveResetPwdCode(String email, String code) {
        redisTemplate.opsForValue().set(prefix2 + email, code, codeTtl);
    }

    public boolean verifyCode(String email, String code) {
        String savedCode = redisTemplate.opsForValue().get(prefix + email);
        return savedCode != null && savedCode.equals(code);
    }

    public boolean verifyResetPwdCode(String email, String code) {
        String savedCode = redisTemplate.opsForValue().get(prefix2 + email);
        return savedCode != null && savedCode.equals(code);
    }

    public void deleteExitedCode(String email) {
        if (redisTemplate.hasKey(prefix + email)) redisTemplate.delete(prefix + email);
    }
}
