package com.culciful.dto;

/**
 * Plain JSON login (HTTPS in prod). RSA-encrypted login can be added later on another path.
 */
public record LoginRequest(String username, String password) {}
