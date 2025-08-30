package com.example.todo.dto;

import com.example.todo.model.Group;
import com.example.todo.model.Schedule;
import com.example.todo.model.Todo;
import com.example.todo.service.GroupService;
import com.example.todo.service.ScheduleService;
import org.springframework.stereotype.Component;

@Component
public class TodoResponseMapper {
    private final GroupService groupService;
    private final ScheduleService scheduleService;

    public TodoResponseMapper(GroupService groupService, ScheduleService scheduleService) {
        this.groupService = groupService;
        this.scheduleService = scheduleService;
    }

    public TodoResponse toTodoResponse(Todo todo) {
        Group group = todo.getGroupId() != null ? groupService.getById(todo.getGroupId()) : null;
        Schedule schedule = todo.getScheduleId() != null ? scheduleService.getById(todo.getScheduleId()) : null;

        return new TodoResponse(
                todo.getId().toString(),
                todo.getContent(),
                todo.getDeadline(),
                todo.getDone(),
                schedule != null ? schedule.getActive() : null,
                schedule != null ? schedule.getFrequency() : null,
                schedule != null ? schedule.getCustomDayOfWeek() : null,
                group != null ? group.getGroupName() : null
        );
    }


}
