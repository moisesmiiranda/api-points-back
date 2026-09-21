package com.mmiranda.pointsbackapi.mail;

/** Port for delivering email. SMTP works with any provider (SES, Resend, Brevo, ...); "log" is for development. */
public interface EmailSender {
    void send(EmailMessage message);
}
