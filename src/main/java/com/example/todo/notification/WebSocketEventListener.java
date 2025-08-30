package com.example.todo.notification;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

@Component
public class  WebSocketEventListener {

    private final TodoNotificationService todoNotificationService;

    public WebSocketEventListener(TodoNotificationService todoNotificationService) {
        this.todoNotificationService = todoNotificationService;
    }

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = accessor.getUser();
        String sessionId = accessor.getSessionId();

        if (principal != null && sessionId != null) {
            String userName = principal.getName();
            try {
                Long userId = Long.parseLong(userName);
                todoNotificationService.registerUser(userId, sessionId);
            } catch (NumberFormatException e) {
                System.err.println("Invalid userId format for WebSocket connection: " + userName);
            }
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = accessor.getUser();
        String sessionId = accessor.getSessionId();

        if (principal != null && sessionId != null) {
            try {
                Long userId = Long.parseLong(principal.getName());
                todoNotificationService.unregisterUser(userId, sessionId);
            } catch (NumberFormatException e) {
                System.err.println("Invalid userId format for WebSocket disconnect: " + principal.getName());
            }
        }
    }
}
