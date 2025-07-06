package com.example.todo.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class HashEncoder {

    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

    public String encode(String value) { return bcrypt.encode(value); }

    public boolean matches(String value, String encoded) { return bcrypt.matches(value, encoded); }
}
