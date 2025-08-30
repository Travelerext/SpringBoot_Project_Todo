package com.example.todo.service;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.todo.model.User;
import org.springframework.stereotype.Service;

@Service
public class UserService extends ServiceImpl<BaseMapper<User>, User> {
}
