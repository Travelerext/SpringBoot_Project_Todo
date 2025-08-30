package com.example.todo.notification;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.todo.mapper.TodoMapper;
import com.example.todo.model.Todo;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

@Service
public class TodoNotificationService {

    private final TodoMapper todoMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final Map<Long, Set<String>> userSessionMap = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public TodoNotificationService(TodoMapper todoMapper, SimpMessagingTemplate messagingTemplate) {
        this.todoMapper = todoMapper;
        this.messagingTemplate = messagingTemplate;
        scheduler.scheduleWithFixedDelay(this::checkAllUpcomingDeadlines, 0, 1, TimeUnit.MINUTES);
    }

    public void registerUser(Long userId, String sessionId) {
        userSessionMap.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet())
                .add(sessionId);
    }

    public void unregisterUser(Long userId, String sessionId) {
        userSessionMap.computeIfPresent(userId, (k, sessions) -> {
            sessions.remove(sessionId);
            return sessions.isEmpty() ? null : sessions;
        });
    }

    private void checkAndNotifyUpcomingDeadlines(Long userId) {
        if (!userSessionMap.containsKey(userId)) return;

        LambdaQueryWrapper<Todo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Todo::getUserId, userId)
                .eq(Todo::getDone, false)
                .eq(Todo::getDeadline, Instant.now().plus(Duration.ofMinutes(15)));

        List<Todo> upcomingTodos = todoMapper.selectList(queryWrapper);
        upcomingTodos.forEach(todo -> sendNotification(userId, todo));
    }

    private void checkAllUpcomingDeadlines() {
        userSessionMap.keySet().forEach(this::checkAndNotifyUpcomingDeadlines);
    }

    private void sendNotification(Long userId, Todo todo) {
        Set<String> sessions = userSessionMap.getOrDefault(userId, Set.of());
        for (String sessionId : sessions) {
            messagingTemplate.convertAndSendToUser(
                    sessionId,
                    "/queue/notifications",
                    todo
            );
        }
    }
}