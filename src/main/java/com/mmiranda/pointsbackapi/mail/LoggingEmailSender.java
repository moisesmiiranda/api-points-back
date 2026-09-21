package com.mmiranda.pointsbackapi.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Development/test sender: writes the email (including its links) to the log and keeps the latest ones in
 * memory. Never used in production: {@link com.mmiranda.pointsbackapi.config.StartupMailGuard} refuses it.
 */
@Component
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "log")
public class LoggingEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailSender.class);
    private static final int KEEP = 50;

    private final List<EmailMessage> recent = new ArrayList<>();

    @Override
    public synchronized void send(EmailMessage message) {
        log.info("[email:dev] to={} subject={}\n{}", message.to(), message.subject(), message.text());
        recent.add(message);
        if (recent.size() > KEEP) {
            recent.remove(0);
        }
    }

    /** Oldest first. */
    public synchronized List<EmailMessage> recentMessages() {
        return List.copyOf(recent);
    }

    public synchronized void clear() {
        recent.clear();
    }
}
