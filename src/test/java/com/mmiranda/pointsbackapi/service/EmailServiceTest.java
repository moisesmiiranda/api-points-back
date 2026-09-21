package com.mmiranda.pointsbackapi.service;

import com.mmiranda.pointsbackapi.mail.EmailMessage;
import com.mmiranda.pointsbackapi.mail.EmailSender;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class EmailServiceTest {

    private final EmailMessage message = new EmailMessage("ana@loja.com", "Assunto", "texto", "<p>html</p>");

    @Test
    void handsTheMessageToTheSender() {
        EmailSender sender = mock(EmailSender.class);

        new EmailService(sender).sendAsync(message);

        verify(sender).send(message);
    }

    @Test
    void aFailingMailServerNeverReachesTheCaller() {
        EmailSender sender = mock(EmailSender.class);
        doThrow(new IllegalStateException("smtp down")).when(sender).send(message);

        assertDoesNotThrow(() -> new EmailService(sender).sendAsync(message));
    }
}
