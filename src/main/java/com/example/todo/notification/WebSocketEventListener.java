package com.example.todo.notification;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

@Component
public class WebSocketEventListener {

    private final TodoNotificationService todoNotificationService;

    public WebSocketEventListener(TodoNotificationService todoNotificationService) {
        this.todoNotificationService = todoNotificationService;
    }

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = headerAccessor.getUser();

        if (principal != null) {
            Long userId = Long.parseLong(principal.getName());
            todoNotificationService.registerUser(userId, headerAccessor.getSessionId());
            todoNotificationService.checkAndNotifyUpcomingDeadlines(userId);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = headerAccessor.getUser();

        if (principal != null) {
            Long userId = Long.parseLong(principal.getName());
            todoNotificationService.unregisterUser(userId);
        }
    }
}
