package com.example.todo.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.List;

@Data
@TableName("schedule")
public class Schedule {
    private Long id;
    private Boolean active;
    private Frequency frequency;
    private List<Short> customDayOfWeek;
}
