package com.example.todo.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.todo.model.Schedule;
import com.example.todo.model.Todo;
import com.example.todo.model.Frequency;
import com.example.todo.model.Group;
import com.example.todo.service.GroupService;
import com.example.todo.service.ScheduleService;
import com.example.todo.service.TodoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/todos")
public class TodoController {

    private final TodoService todoService;
    private final GroupService groupService;
    private final ScheduleService scheduleService;

    public TodoController(TodoService todoService, GroupService groupService, ScheduleService scheduleService) {
        this.todoService = todoService;
        this.groupService = groupService;
        this.scheduleService = scheduleService;
    }

    public record TodoRequest(String content, Instant deadline, Frequency frequency, List<Short> customDayOfWeek) {}

    public record TodoResponse(
            Long id, 
            String content, 
            Instant deadline, 
            Frequency frequency, 
            List<Short> customDayOfWeek,
            String groupName
    ) {}

    @GetMapping("/today")
    public ResponseEntity<Page<TodoResponse>> getTodayTodos(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Page<Todo> todoPage = todoService.getTodayTodos(current, size, userId);
        Page<TodoResponse> responsePage = new Page<>(todoPage.getCurrent(), todoPage.getSize(), todoPage.getTotal());
        responsePage.setRecords(todoPage.getRecords().stream().map(this::toResponse).collect(Collectors.toList()));
        return ResponseEntity.ok(responsePage);
    }

    @GetMapping("/undone")
    public ResponseEntity<Page<TodoResponse>> getAllUndoneTodos(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Page<Todo> todoPage = todoService.getAllUndoneTodos(current, size, userId);
        Page<TodoResponse> responsePage = new Page<>(todoPage.getCurrent(), todoPage.getSize(), todoPage.getTotal());
        responsePage.setRecords(todoPage.getRecords().stream().map(this::toResponse).collect(Collectors.toList()));
        return ResponseEntity.ok(responsePage);
    }

    @GetMapping("/overdue")
    public ResponseEntity<Page<TodoResponse>> getOverdueTodos(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Page<Todo> todoPage = todoService.getOverdueTodos(current, size, userId);
        Page<TodoResponse> responsePage = new Page<>(todoPage.getCurrent(), todoPage.getSize(), todoPage.getTotal());
        responsePage.setRecords(todoPage.getRecords().stream().map(this::toResponse).collect(Collectors.toList()));
        return ResponseEntity.ok(responsePage);
    }

    @GetMapping("/done")
    public ResponseEntity<Page<TodoResponse>> getAllDoneTodos(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Page<Todo> todoPage = todoService.getAllDoneTodos(current, size, userId);
        Page<TodoResponse> responsePage = new Page<>(todoPage.getCurrent(), todoPage.getSize(), todoPage.getTotal());
        responsePage.setRecords(todoPage.getRecords().stream().map(this::toResponse).collect(Collectors.toList()));
        return ResponseEntity.ok(responsePage);
    }

    @GetMapping("/schedule")
    public ResponseEntity<Schedule> getScheduleByTodo(@RequestParam Long todoId) {
        Todo todo = new Todo();
        todo.setId(todoId);
        return ResponseEntity.ok(todoService.getScheduleByTodo(todo));
    }

    @PostMapping("/create")
    public ResponseEntity<Boolean> createTodo(@RequestBody TodoRequest requestBody) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(todoService.createTodo(
                requestBody.content,
                userId,
                requestBody.deadline,
                requestBody.frequency,
                requestBody.customDayOfWeek
        ));
    }

    @PutMapping("/edit")
    public ResponseEntity<Boolean> editTodo(@RequestParam Long todoId, @RequestBody TodoRequest requestBody) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(todoService.editTodo(
                todoId,
                requestBody.content,
                userId,
                requestBody.deadline,
                requestBody.frequency,
                requestBody.customDayOfWeek
        ));
    }

    @PutMapping("/mark_done")
    public ResponseEntity<Boolean> markDoneTodo(@RequestParam Long todoId) {
        return ResponseEntity.ok(todoService.markDoneTodo(todoId));
    }

    @DeleteMapping("/{todoId}")
    public ResponseEntity<Boolean> deleteTodo(@PathVariable Long todoId) {
        return ResponseEntity.ok(todoService.deleteTodo(todoId));
    }

    @PutMapping("/cancel_schedule")
    public ResponseEntity<Boolean> cancelSchedule(@RequestParam Long scheduleId) {
        return ResponseEntity.ok(todoService.cancelSchedule(scheduleId));
    }

    private TodoController.TodoResponse toResponse(Todo todo) {
        Group group = todo.getGroupId() != null ? groupService.getById(todo.getGroupId()) : null;
        Schedule schedule = todo.getScheduleId() != null ? scheduleService.getById(todo.getScheduleId()) : null;
        return new TodoController.TodoResponse(
                todo.getId(),
                todo.getContent(),
                todo.getDeadline(),
                schedule != null ? schedule.getFrequency() : null,
                schedule != null ? schedule.getCustomDayOfWeek() : null,
                group != null ? group.getGroupName() : null
        );
    }
}
