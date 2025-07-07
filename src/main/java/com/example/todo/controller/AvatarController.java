package com.example.todo.controller;

import com.example.todo.mapper.UserMapper;
import com.example.todo.model.Avatar;
import com.example.todo.model.User;
import com.example.todo.service.AvatarService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/avatar")
public class AvatarController {

    private final UserMapper userMapper;
    private final AvatarService avatarService;

    public AvatarController(UserMapper userMapper, AvatarService avatarService) {
        this.userMapper = userMapper;
        this.avatarService = avatarService;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadImage(
            @RequestParam("file") MultipartFile file
    ) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        try {
            Avatar avatar = avatarService.uploadAvatar(file, userId);

            return ResponseEntity
                    .ok(
                            Map.ofEntries(
                                    Map.entry("imageId", avatar.getId()),
                                    Map.entry("originalName", avatar.getOriginName()),
                                    Map.entry("size", avatar.getSize())
                            )
                    );
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/myAvatar")
    public ResponseEntity<?> downloadImage() {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        try {
            User user = userMapper.selectById(userId);
            if (user == null) { throw new IllegalArgumentException("User not found"); }

            Avatar avatar = avatarService.getAvatar(user.getAvatarId());
            Resource resource = avatarService.getAvatarResource(user.getAvatarId());

            return ResponseEntity
                    .ok()
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + avatar.getOriginName() + "\""
                    )
                    .contentType(MediaType.parseMediaType(avatar.getMimeType()))
                    .contentLength(avatar.getSize())
                    .body(resource);
        } catch(IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}
