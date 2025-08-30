package com.example.todo.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.Instant;

@Data
@TableName("todo")
public class Todo {
    private Long id;
    private String content;
    private Boolean done;
    private Long userId;
    private Long groupId;
    private Long scheduleId;
    private Instant deadline;
    private Instant createdAt = Instant.now();
}
