package com.spad.security;

/** The authenticated principal extracted from a valid JWT. studentId is null for faculty. */
public record AuthUser(Long userId, String email, String role, Long studentId) {}
