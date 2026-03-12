package com.example.shared.data;

import java.io.Serializable;

/**
 * Data record representing a user.
 *
 * @param username the user's username
 * @param password the user's password
 */
public record User(String username, String password) implements Serializable {}
