package com.example.todo.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@TableName("`group`")
public class Group {
    private Long id;
    private String groupName;
    private Long userId;
    private Instant createdAt = Instant.now();
}
