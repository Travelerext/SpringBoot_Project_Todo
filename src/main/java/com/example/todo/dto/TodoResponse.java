package com.example.todo.dto;

import com.example.todo.model.Frequency;

import java.time.Instant;
import java.util.List;

public record TodoResponse(
        String id,
        String content,
        Instant deadline,
        Boolean done,
        Boolean active,
        Frequency frequency,
        List<Short> customDayOfWeek,
        String groupName
) {}
