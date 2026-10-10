package com.spad.service;

import com.spad.dto.Dtos.AuthResponse;
import com.spad.dto.Dtos.LoginRequest;
import com.spad.dto.Dtos.UserDto;
import com.spad.exception.ApiException;
import com.spad.model.Student;
import com.spad.model.User;
import com.spad.repository.StudentRepository;
import com.spad.repository.UserRepository;
import com.spad.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository users;
    private final StudentRepository students;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UserRepository users, StudentRepository students, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.students = students;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        User user = users.findByEmail(req.email().trim().toLowerCase())
                .filter(u -> encoder.matches(req.password(), u.getPasswordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        Long studentId = students.findByUserId(user.getId()).map(Student::getId).orElse(null);
        return new AuthResponse(jwt.generate(user, studentId),
                new UserDto(user.getId(), user.getName(), user.getEmail(), user.getRole().name(), studentId));
    }
}
