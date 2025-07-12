package com.example.todo.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.todo.mapper.RefreshTokenMapper;
import com.example.todo.mapper.UserMapper;
import com.example.todo.model.RefreshToken;
import com.example.todo.model.User;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;

@Service
public class AuthService {

    private final JwtService jwtService;
    private final HashEncoder hashEncoder;
    private final RefreshTokenMapper refreshTokenMapper;
    private final UserMapper userMapper;
    private final VerifyCodeService verifyCodeService;
    public final EmailService emailService;

    public AuthService(JwtService jwtService, HashEncoder hashEncoder, RefreshTokenMapper refreshTokenMapper, UserMapper userMapper, VerifyCodeService verifyCodeService, EmailService emailService) {
        this.jwtService = jwtService;
        this.hashEncoder = hashEncoder;
        this.refreshTokenMapper = refreshTokenMapper;
        this.userMapper = userMapper;
        this.verifyCodeService = verifyCodeService;
        this.emailService = emailService;
    }

    public record TokenPair(String accessToken, String refreshToken) {}

    public void sendVerifyCode(String email) throws ResponseStatusException, MailException {
        String trimmedEmail = email.trim();
        User existingEmailUser = userMapper.selectOne(
                (new LambdaQueryWrapper<User>()).eq(User::getEmail, trimmedEmail)
        );
        if (existingEmailUser != null) throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already taken");
        String code = verifyCodeService.generateCode();
        verifyCodeService.saveCode(trimmedEmail, code);
        String content = String.format("Your verification code is %s", code);
        emailService.sendEmail(trimmedEmail, "Verify your email", content);
    }

    public User register(String username, String email, String password, String inputCode) throws ResponseStatusException {
        String trimmedUsername = username.trim();
        User existingUser = userMapper.selectOne(
                (new LambdaQueryWrapper<User>()).eq(User::getUserName, trimmedUsername)
        );
        if (existingUser != null) throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        String trimmedEmail = email.trim();
        User existingEmailUser = userMapper.selectOne(
                (new LambdaQueryWrapper<User>()).eq(User::getEmail, trimmedEmail)
        );
        if (existingEmailUser != null) throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already taken");

        if (!verifyCodeService.verifyCode(email, inputCode)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid verification code");

        User user = new User();
        user.setUserName(trimmedUsername);
        user.setEmail(trimmedEmail);
        user.setPassword(hashEncoder.encode(password));
        user.setCreatedAt(Instant.now());
        userMapper.insert(user);
        return user;
    }

    public TokenPair login(String email, String password) throws ResponseStatusException {
        User user = userMapper.selectOne(
                (new LambdaQueryWrapper<User>()).eq(User::getEmail, email).or()
                .eq(User::getUserName, email)
        );
        if (user == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid user");
        if (!hashEncoder.matches(password, user.getPassword())) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid password");
        String accessToken = jwtService.generateAccessToken(user.getId());
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        storeRefreshToken(user.getId(), refreshToken);
        return new TokenPair(accessToken, refreshToken);
    }

    @Transactional
    public TokenPair refresh(String token) throws ResponseStatusException {
        if (!jwtService.validateRefreshToken(token)) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        Long userId = jwtService.getUserIdFromToken(token);
        String hashed = hashToken(token);
        RefreshToken record = refreshTokenMapper.selectOne(
                (new QueryWrapper<RefreshToken>()).eq("hashed_token", hashed).eq("user_id", userId)
        );
        if (record == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        if (record.getExpireAt().isBefore(Instant.now())) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expired");
        refreshTokenMapper.delete(
                (new QueryWrapper<RefreshToken>()).eq("hashed_token", hashed).eq("user_id", userId)
        );
        String newAccessToken = jwtService.generateAccessToken(userId);
        String newRefreshToken = jwtService.generateRefreshToken(userId);
        storeRefreshToken(userId, newRefreshToken);
        return new TokenPair(newAccessToken, newRefreshToken);
    }

    @Scheduled(fixedRate = 1000 * 60 * 60)
    private void deleteExpiredRefreshTokens() {
        Instant now = Instant.now();
        refreshTokenMapper.delete(
                (new QueryWrapper<RefreshToken>()).lt("expire_at", now)
        );
    }

    private void storeRefreshToken(Long userId, String token) {
        String hashedToken = hashToken(token);
        long expiryMs = jwtService.refreshTokenValidityMs;
        Instant expireAt = Instant.now().plusMillis(expiryMs);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setHashedToken(hashedToken);
        refreshToken.setUserId(userId);
        refreshToken.setExpireAt(expireAt);
        refreshToken.setCreatedAt(Instant.now());
        refreshTokenMapper.insert(refreshToken);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(token.getBytes());
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }


}
