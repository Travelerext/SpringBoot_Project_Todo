package com.example.todo.notification;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.todo.mapper.TodoMapper;
import com.example.todo.model.Todo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Service
public class TodoNotificationService {

    private final TodoMapper todoMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final Map<Long, String> userSessionMap = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(1);
    public TodoNotificationService(TodoMapper todoMapper, SimpMessagingTemplate messagingTemplate) {
        this.todoMapper = todoMapper;
        this.messagingTemplate = messagingTemplate;
        scheduler.scheduleWithFixedDelay(this::checkAllUpcomingDeadlines, 0, 1, TimeUnit.MINUTES);
    }

    public void registerUser(Long userId, String sessionId) {
        userSessionMap.put(userId, sessionId);
    }

    public void unregisterUser(Long userId) {
        userSessionMap.remove(userId);
    }

    public void checkAndNotifyUpcomingDeadlines(Long userId) {
        if (!userSessionMap.containsKey(userId)) {
            return;
        }

        LambdaQueryWrapper<Todo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Todo::getUserId, userId)
                .eq(Todo::getDone, false)
                .le(Todo::getDeadline, Instant.now().plus(Duration.ofMinutes(15)));

        List<Todo> upcomingTodos = todoMapper.selectList(queryWrapper);

        for (Todo todo : upcomingTodos) {
            sendNotification(userId, todo);
        }
    }

    private void checkAllUpcomingDeadlines() {
        for (Long userId : userSessionMap.keySet()) {
            checkAndNotifyUpcomingDeadlines(userId);
        }
    }

    private void sendNotification(Long userId, Todo todo) {

        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/notifications",
                todo
        );
    }
}