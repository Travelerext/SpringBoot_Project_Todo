package com.example.todo.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.Instant;

@Data
@TableName("user")
public class User {
    private Long id;
    private String userName;
    private String email;
    private String password;
    private Long avatarId;
    private Instant createdAt = Instant.now();
}
