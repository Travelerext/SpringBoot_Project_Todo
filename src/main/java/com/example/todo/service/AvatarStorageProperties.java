package com.example.todo.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Set;

@ConfigurationProperties(prefix = "avatar-storage")
@Component
public record AvatarStorageProperties(
        String basePath,
        Set<String> allowedMimeTypes
) {
    public AvatarStorageProperties() {
        this(
                "./images",
                Set.of(
                        "image/jpeg",
                        "image/png",
                        "image/webp",
                        "image/gif"
                )
        );
    }
}
