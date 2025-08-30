package com.example.todo.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.util.List;

@Data
@TableName(value = "schedule", autoResultMap = true)
public class Schedule {
    private Long id;
    private Boolean active;
    private Frequency frequency;
    @TableField(value = "custom_day_of_week", typeHandler = JacksonTypeHandler.class)
    private List<Short> customDayOfWeek;
}
