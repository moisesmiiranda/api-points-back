package com.mmiranda.pointsbackapi.service;

import com.mmiranda.pointsbackapi.mail.EmailMessage;
import com.mmiranda.pointsbackapi.mail.EmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Sends email off the request thread. The caller never waits for the mail server and never sees its
 * failures, so responses (for example "forgot password") take the same time and answer the same way
 * whether or not an email was really sent.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final EmailSender sender;

    public EmailService(EmailSender sender) {
        this.sender = sender;
    }

    @Async
    public void sendAsync(EmailMessage message) {
        try {
            sender.send(message);
        } catch (RuntimeException e) {
            // Logged without the body: it holds a live link
            log.error("Failed to send email '{}' to {}", message.subject(), message.to(), e);
        }
    }
}
