package com.example.todo.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.Instant;

@Data
@TableName("refresh_token")
public class RefreshToken {
    private Long id;
    private String hashedToken;
    private Long userId;
    private Instant expireAt;
    private Instant createdAt;
}
