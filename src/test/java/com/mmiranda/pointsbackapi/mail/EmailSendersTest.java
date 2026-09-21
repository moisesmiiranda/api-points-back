package com.mmiranda.pointsbackapi.mail;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailSendersTest {

    private final EmailMessage message = new EmailMessage("ana@loja.com", "Assunto", "texto", "<p>html</p>");

    @Test
    void loggingSenderKeepsTheLatestMessagesForDevelopment() {
        LoggingEmailSender sender = new LoggingEmailSender();

        for (int i = 0; i < 60; i++) {
            sender.send(new EmailMessage("u" + i + "@x.com", "s", "t", "h"));
        }

        assertEquals(50, sender.recentMessages().size());
        assertEquals("u10@x.com", sender.recentMessages().get(0).to());
        sender.clear();
        assertEquals(0, sender.recentMessages().size());
    }

    @Test
    void smtpSenderBuildsAMultipartMessageFromTheConfiguredAddress() throws Exception {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        MimeMessage mime = new JavaMailSenderImpl().createMimeMessage();
        when(mailSender.createMimeMessage()).thenReturn(mime);

        new SmtpEmailSender(mailSender, "no-reply@pointsback.com.br").send(message);

        ArgumentCaptor<MimeMessage> sent = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(sent.capture());
        assertEquals("Assunto", sent.getValue().getSubject());
        assertEquals("ana@loja.com", sent.getValue().getAllRecipients()[0].toString());
        assertEquals("no-reply@pointsback.com.br", sent.getValue().getFrom()[0].toString());
    }

    @Test
    void smtpFailuresBecomeAnUncheckedError() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        when(mailSender.createMimeMessage()).thenReturn(new JavaMailSenderImpl().createMimeMessage());
        doThrow(new MailSendException("smtp down")).when(mailSender).send(any(MimeMessage.class));

        assertThrows(IllegalStateException.class,
                () -> new SmtpEmailSender(mailSender, "a@b.com").send(message));
    }
}
