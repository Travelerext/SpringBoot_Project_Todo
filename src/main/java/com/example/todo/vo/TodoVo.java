package com.example.todo.vo;

import com.example.todo.model.Frequency;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class TodoVo {
    private Long id;
    private String content;
    private Instant deadline;
    private Frequency frequency;
    private List<Short> customDayOfWeek;
    private String groupName;
}