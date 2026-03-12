package com.example.shared.data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Data record representing an email.
 *
 * @param id        the unique identifier of the email
 * @param sender    the user who sent the email
 * @param receivers the list of users who receive the email
 * @param subject   the subject line of the email
 * @param text      the content body of the email
 * @param date      the timestamp when the email was sent
 */
public record Email(String id, User sender, List <User> receivers, String subject, String text, LocalDateTime date)
        implements Serializable {}
