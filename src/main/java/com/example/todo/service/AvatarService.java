package com.example.todo.service;

import com.example.todo.mapper.AvatarMapper;
import com.example.todo.mapper.UserMapper;
import com.example.todo.model.Avatar;
import com.example.todo.model.User;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

@Service
public class AvatarService {

    private final AvatarStorageProperties properties;
    private final AvatarStorageService storageService;
    private final AvatarMapper avatarMapper;
    private final UserMapper userMapper;


    public AvatarService(AvatarStorageProperties properties, AvatarStorageService storageService, AvatarMapper avatarMapper, UserMapper userMapper) {
        this.properties = properties;
        this.storageService = storageService;
        this.avatarMapper = avatarMapper;
        this.userMapper = userMapper;
    }

    @Transactional
    public Avatar uploadAvatar(MultipartFile file, Long userId) throws IOException, IllegalArgumentException {
        User user = userMapper.selectById(userId);
        if (user == null) { throw  new IllegalArgumentException("User not found"); }

        validateFileExtension(file);

        String storagePath;
        try(InputStream inputStream = file.getInputStream()) {
            storagePath = storageService.storeFile(inputStream, file.getOriginalFilename());
        }

        Avatar avatar = new Avatar();
        avatar.setOriginName(file.getOriginalFilename());
        avatar.setStorageName(storagePath);
        avatar.setMimeType(file.getContentType());
        avatar.setSize(file.getSize());

        avatarMapper.insert(avatar);
        Avatar oldAvatar = avatarMapper.selectById(user.getAvatarId());
        user.setAvatarId(avatar.getId());
        userMapper.updateById(user);
        if (oldAvatar != null) {
            avatarMapper.deleteById(oldAvatar);
            Files.deleteIfExists(storageService.getFileResource(oldAvatar.getStorageName()).getFile().toPath());
        }

        return avatar;
    }

    public Resource getAvatarResource(Long avatarId) throws IOException {
        Avatar avatar = getAvatar(avatarId);
        return storageService.getFileResource(avatar.getStorageName());
    }

    public Avatar getAvatar(Long avatarId) {
        return avatarMapper.selectById(avatarId);
    }

    private void validateFileExtension(MultipartFile file) {
        if(file.isEmpty()) {
            throw new IllegalArgumentException("File is empty.");
        }

        String mimeType = file.getContentType();
        if(mimeType == null || !properties.allowedMimeTypes().contains(mimeType)) {
            throw new IllegalArgumentException("Invalid mime type.");
        }
    }
}
