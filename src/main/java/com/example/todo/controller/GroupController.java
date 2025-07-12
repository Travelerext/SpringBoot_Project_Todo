package com.example.todo.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.todo.model.Group;
import com.example.todo.service.GroupService;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    public record CreateGroupRequest(String groupName, List<Long> todoIds) {}

    public record EditGroupRequest(Long groupId, String groupName) {}

    public record EditTodosRequest(Long groupId, List<Long> todoIds) {}

    @GetMapping("/my_group")
    public ResponseEntity<List<Group>> getMyGroups() {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(groupService.getGroups(userId));
    }

    @PostMapping("/create")
    public ResponseEntity<Boolean> createGroup(@RequestBody CreateGroupRequest requestBody) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(groupService.createGroup(requestBody.groupName, userId, requestBody.todoIds));
    }

    @PutMapping("/edit_group")
    public ResponseEntity<Boolean> updateGroup(@RequestBody EditGroupRequest requestBody) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(groupService.editGroupName(requestBody.groupId, requestBody.groupName, userId));
    }

    @DeleteMapping("/{groupId}")
    public ResponseEntity<Boolean> deleteGroup(@PathVariable Long groupId) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(groupService.deleteGroup(groupId, userId));
    }

    @PutMapping("/add_todos")
    public ResponseEntity<Boolean> addTodoToGroup(@RequestBody EditTodosRequest requestBody) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(groupService.addTodosIntoGroup(requestBody.todoIds, requestBody.groupId, userId));
    }

    @DeleteMapping("/remove_todos")
    public ResponseEntity<Boolean> removeTodoFromGroup(@RequestBody EditTodosRequest requestBody) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(groupService.removeTodosFromGroup(requestBody.todoIds, requestBody.groupId, userId));
    }
}
