package com.example.shared.data;

import java.io.Serializable;
import java.util.List;

/**
 * Data record representing a network transmission package.
 *
 * @param type       the type of the transmission
 * @param user       the user context
 * @param email      the email context (if applicable)
 * @param email_list the list of emails (if applicable)
 * @param message    the message content (if applicable)
 */
public record Package(TYPE type, User user, Email email, List <Email> email_list, String message)
        implements Serializable {}