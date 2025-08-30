package com.example.todo.controller;

import com.example.todo.dto.TodoResponse;
import com.example.todo.dto.TodoResponseMapper;
import com.example.todo.mapper.TodoMapper;
import com.example.todo.model.Group;
import com.example.todo.service.GroupService;
import com.example.todo.service.TodoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groupService;
    private final TodoService todoService;
    private final TodoResponseMapper toTodoResponseMapper;

    public GroupController(GroupService groupService, TodoMapper todoMapper, TodoService todoService, TodoResponseMapper toTodoResponseMapper) {
        this.groupService = groupService;
        this.todoService = todoService;
        this.toTodoResponseMapper = toTodoResponseMapper;
    }

    public record CreateGroupRequest(String groupName, List<String> todoIds) {}

    public record EditGroupRequest(String groupId, String groupName) {}

    public record EditTodosRequest(String groupId, List<Long> todoIds) {}

    public record GroupResponse(String groupId, String groupName, List<TodoResponse> todoIds) {}

    @GetMapping("/my_group")
    public ResponseEntity<List<Group>> getMyGroups() {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(groupService.getGroups(userId));
    }

    @PostMapping("/create")
    public ResponseEntity<?> createGroup(@RequestBody CreateGroupRequest requestBody) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(
                toResponse(
                        groupService.createGroup(
                                requestBody.groupName,
                                userId,
                                requestBody.todoIds.stream().map(Long::parseLong).toList()
                        )
                )
        );
    }

    @PutMapping("/edit_group")
    public ResponseEntity<?> editGroupName(@RequestBody EditGroupRequest requestBody) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(
                toResponse(
                        groupService.editGroupName(
                                Long.parseLong(requestBody.groupId),
                                requestBody.groupName,
                                userId
                        )
                )
        );
    }

    @DeleteMapping("/{groupId}")
    public ResponseEntity<?> deleteGroup(@PathVariable String groupId) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        try {
            groupService.deleteGroup(Long.parseLong(groupId), userId);
            return ResponseEntity.ok(null);
        } catch (ResponseStatusException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/add_todos")
    public ResponseEntity<?> addTodosToGroup(@RequestBody EditTodosRequest requestBody) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        try {

            return ResponseEntity.ok(
                    toResponse(
                            groupService.addTodosIntoGroup(
                                requestBody.todoIds,
                                Long.parseLong(requestBody.groupId),
                                userId
                            )
                    )
            );
        } catch (ResponseStatusException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/remove_todos")
    public ResponseEntity<?> removeTodosFromGroup(@RequestBody EditTodosRequest requestBody) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        try {
            groupService.removeTodosFromGroup(
                    requestBody.todoIds,
                    Long.parseLong(requestBody.groupId),
                    userId
            );
            return ResponseEntity.ok(null);
        } catch (ResponseStatusException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private GroupResponse toResponse(Group group) {
         List<TodoResponse> todoResponses = todoService.findTodosByGroup(group.getId())
                 .stream()
                 .map(toTodoResponseMapper::toTodoResponse)
                 .toList();

         return new GroupResponse(
                 group.getId().toString(),
                 group.getGroupName(),
                 todoResponses
         );
    }
}
