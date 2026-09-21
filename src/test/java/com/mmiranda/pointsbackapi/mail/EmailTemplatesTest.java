package com.mmiranda.pointsbackapi.mail;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailTemplatesTest {

    private static final String LINK = "https://app.exemplo.com.br/redefinir-senha?token=abc_DEF-123";

    @Test
    void passwordResetHasTheLinkInBothBodiesAndSaysHowLongItLasts() {
        EmailMessage message = EmailTemplates.passwordReset("ana@loja.com", "Ana", LINK, 60);

        assertEquals("ana@loja.com", message.to());
        assertEquals("Redefinição de senha - Points Manager", message.subject());
        assertTrue(message.text().contains(LINK));
        assertTrue(message.text().contains("1 hora"));
        assertTrue(message.html().contains("href=\"" + LINK + "\""));
        assertTrue(message.text().contains("ignore este e-mail"));
    }

    @Test
    void inviteMentionsTheEstablishmentWhenThereIsOne() {
        EmailMessage withShop = EmailTemplates.inviteWithLink("a@b.com", "Ana", "Padaria Boa", LINK, 72);
        EmailMessage withoutShop = EmailTemplates.inviteWithLink("a@b.com", "Ana", null, LINK, 72);

        assertTrue(withShop.text().contains("do estabelecimento Padaria Boa"));
        assertTrue(withShop.text().contains("72 horas"));
        assertFalse(withoutShop.text().contains("estabelecimento"));
        assertTrue(withoutShop.text().contains(LINK));
    }

    @Test
    void temporaryPasswordWelcomeNeverContainsAPasswordOrAResetLink() {
        EmailMessage message = EmailTemplates.welcomeWithTemporaryPassword("a@b.com", "Ana", "Padaria", "https://app.exemplo.com.br/login");

        assertTrue(message.text().contains("https://app.exemplo.com.br/login"));
        assertTrue(message.text().contains("senha provisória"));
        assertFalse(message.text().contains("token="));
    }

    @Test
    void valuesFromUsersAreEscapedInTheHtmlBody() {
        EmailMessage message = EmailTemplates.passwordReset("a@b.com", "<script>alert('x')</script> & Cia", LINK, 30);

        assertFalse(message.html().contains("<script>"));
        assertTrue(message.html().contains("&lt;script&gt;alert(&#39;x&#39;)&lt;/script&gt; &amp; Cia"));
        assertTrue(message.text().contains("30 minutos"));
    }

    @Test
    void humanDurationAndEscapeHandleEdgeCases() {
        assertEquals("2 horas", EmailTemplates.humanDuration(120));
        assertEquals("1 hora", EmailTemplates.humanDuration(60));
        assertEquals("45 minutos", EmailTemplates.humanDuration(45));
        assertEquals("", EmailTemplates.escape(null));
    }
}
