package com.example.todo.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.Instant;

@Data
@TableName("avatar")
public class Avatar {
    private Long id;
    private String originName;
    private String storageName;
    private String mimeType;
    private Long size;
    private Instant createdAt = Instant.now();
}
