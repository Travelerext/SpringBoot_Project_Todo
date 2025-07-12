package com.example.todo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.todo.mapper.GroupMapper;
import com.example.todo.mapper.TodoMapper;
import com.example.todo.model.Group;
import com.example.todo.model.Todo;
import org.springframework.stereotype.Service;

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

    public Boolean createGroup(String groupName, Long userId, List<Long> todoIds) {
        Group group = new Group();
        group.setGroupName(groupName);
        group.setUserId(userId);
        group.setCreatedAt(Instant.now());
        boolean result = groupMapper.insert(group) == 1;

        if (result && todoIds != null && !todoIds.isEmpty()) {
            for (Long todoId : todoIds) {
                Todo todo = todoMapper.selectById(todoId);
                if (todo != null && Objects.equals(todo.getUserId(), userId)) {
                    todo.setGroupId(group.getId());
                    todoMapper.updateById(todo);
                }
            }
        }

        return result;
    }

    public List<Group> getGroups(Long userId) {
        return groupMapper.selectList(
                new LambdaQueryWrapper<Group>()
                        .eq(Group::getUserId, userId)
                        .orderByDesc(Group::getCreatedAt)
        );
    }

    public Boolean addTodosIntoGroup(List<Long> todoIds, Long groupId, Long userId) {
        Group group = groupMapper.selectById(groupId);
        if (group == null || !Objects.equals(group.getUserId(), userId)) {
            return false;
        }

        for (Long todoId : todoIds) {
            Todo todo = todoMapper.selectById(todoId);
            if (todo != null && Objects.equals(todo.getUserId(), userId)) {
                todo.setGroupId(groupId);
                todoMapper.updateById(todo);
            }
        }

        return true;
    }

    public Boolean removeTodosFromGroup(List<Long> todoIds, Long groupId, Long userId) {
        Group group = groupMapper.selectById(groupId);
        if (group == null || !Objects.equals(group.getUserId(), userId)) {
            return false;
        }

        for (Long todoId : todoIds) {
            Todo todo = todoMapper.selectById(todoId);
            if (todo != null && Objects.equals(todo.getUserId(), userId) && Objects.equals(todo.getGroupId(), groupId)) {
                todo.setGroupId(null);
                todoMapper.updateById(todo);
            }
        }

        return true;
    }

    public Boolean deleteGroup(Long groupId, Long userId) {
        Group group = groupMapper.selectById(groupId);
        if (group == null || !Objects.equals(group.getUserId(), userId)) {
            return false;
        }

        todoMapper.update(null, new LambdaUpdateWrapper<Todo>()
                .eq(Todo::getGroupId, groupId)
                .set(Todo::getGroupId, null));

        return groupMapper.deleteById(groupId) == 1;
    }

    public Boolean editGroupName(Long groupId, String groupName, Long userId) {
        Group group = groupMapper.selectById(groupId);
        if (group == null || !Objects.equals(group.getUserId(), userId)) {
            return false;
        }

        group.setGroupName(groupName);
        return groupMapper.updateById(group) == 1;
    }
}
