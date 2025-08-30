package com.example.todo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.todo.mapper.GroupMapper;
import com.example.todo.mapper.TodoMapper;
import com.example.todo.model.Group;
import com.example.todo.model.Todo;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
public class GroupService extends ServiceImpl<BaseMapper<Group>, Group> {

    private final GroupMapper groupMapper;
    private final TodoMapper todoMapper;

    public GroupService(GroupMapper groupMapper, TodoMapper todoMapper) {
        this.groupMapper = groupMapper;
        this.todoMapper = todoMapper;
    }

    @Transactional
    public Group createGroup(String groupName, Long userId, List<Long> todoIds) throws ResponseStatusException {
        Group group = new Group();
        group.setGroupName(groupName);
        group.setUserId(userId);
        group.setCreatedAt(Instant.now());
        if (groupMapper.insert(group) != 1) throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to insert group");

        todoIds.forEach(todoId -> {
            Todo todo = todoMapper.selectById(todoId);
            if (todo == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid todo id: " + todoId);
            todo.setGroupId(group.getId());
            if (todoMapper.updateById(todo) != 1) throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update todo");
        });
        return group;
    }

    public List<Group> getGroups(Long userId) {
        return groupMapper.selectList(
                new LambdaQueryWrapper<Group>()
                        .eq(Group::getUserId, userId)
                        .orderByDesc(Group::getCreatedAt)
        );
    }

    @Transactional
    public Group addTodosIntoGroup(List<Long> todoIds, Long groupId, Long userId) throws ResponseStatusException {
        Group group = groupMapper.selectById(groupId);
        if (group == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid group id");
        if (!Objects.equals(group.getUserId(), userId)) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid user id");

        todoIds.forEach(todoId -> {
            Todo todo = todoMapper.selectById(todoId);
            if (todo == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid todo id: " + todoId);
            if (!Objects.equals(todo.getUserId(), userId)) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid user id");
            todo.setGroupId(group.getId());
            if (todoMapper.updateById(todo) != 1) throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update todo");
        });

        return group;
    }

    @Transactional
    public void removeTodosFromGroup(List<Long> todoIds, Long groupId, Long userId) throws ResponseStatusException {
        Group group = groupMapper.selectById(groupId);
        if (group == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid group id");
        if (!Objects.equals(group.getUserId(), userId)) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid user id");

        todoIds.forEach(todoId -> {
            Todo todo = todoMapper.selectById(todoId);
            if (todo == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid todo id: " + todoId);
            if (!Objects.equals(todo.getUserId(), userId)) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid user id");
            todo.setGroupId(null);

            if (todoMapper.updateById(todo) != 1) throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update todo");
        });
    }

    public void deleteGroup(Long groupId, Long userId) throws ResponseStatusException {
        Group group = groupMapper.selectById(groupId);
        if (group == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid group id");
        if (!Objects.equals(group.getUserId(), userId)) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid user id");

        if (groupMapper.deleteById(group) != 1) throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete group");
    }

    public Group editGroupName(Long groupId, String groupName, Long userId) {
        Group group = groupMapper.selectById(groupId);
        if (group == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid group id");
        if (!Objects.equals(group.getUserId(), userId)) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid user id");

        group.setGroupName(groupName);
        if (groupMapper.updateById(group) != 1) throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update group");

        return group;
    }
}
