package com.mmiranda.pointsbackapi.mail;

/** The pt-BR emails the system sends. Every value that comes from a user is HTML-escaped. */
public final class EmailTemplates {

    private EmailTemplates() {
    }

    public static EmailMessage passwordReset(String to, String name, String link, long validForMinutes) {
        String text = "Olá, " + name + "!\n\n"
                + "Recebemos um pedido para redefinir a senha da sua conta no Points Manager.\n"
                + "Para escolher uma nova senha, abra o link abaixo (vale por " + humanDuration(validForMinutes) + " e só pode ser usado uma vez):\n\n"
                + link + "\n\n"
                + "Se você não pediu isso, ignore este e-mail: sua senha continua a mesma.";
        String html = layout("Redefinir sua senha",
                "<p>Olá, " + escape(name) + "!</p>"
                        + "<p>Recebemos um pedido para redefinir a senha da sua conta no Points Manager.</p>"
                        + button(link, "Escolher nova senha")
                        + "<p>O link vale por " + humanDuration(validForMinutes) + " e só pode ser usado uma vez.</p>"
                        + "<p>Se você não pediu isso, ignore este e-mail: sua senha continua a mesma.</p>");
        return new EmailMessage(to, "Redefinição de senha - Points Manager", text, html);
    }

    /** Welcome for an account created without a password: the link lets the person choose one. */
    public static EmailMessage inviteWithLink(String to, String name, String establishment, String link, long validForHours) {
        String where = establishment != null ? " do estabelecimento " + establishment : "";
        String text = "Olá, " + name + "!\n\n"
                + "Foi criada uma conta" + where + " para você no Points Manager.\n"
                + "Para definir a sua senha e entrar, abra o link abaixo (vale por " + validForHours + " horas e só pode ser usado uma vez):\n\n"
                + link + "\n\n"
                + "Se você não esperava este e-mail, pode ignorá-lo.";
        String html = layout("Bem-vindo ao Points Manager",
                "<p>Olá, " + escape(name) + "!</p>"
                        + "<p>Foi criada uma conta" + escape(where) + " para você no Points Manager.</p>"
                        + button(link, "Definir minha senha")
                        + "<p>O link vale por " + validForHours + " horas e só pode ser usado uma vez.</p>"
                        + "<p>Se você não esperava este e-mail, pode ignorá-lo.</p>");
        return new EmailMessage(to, "Sua conta no Points Manager", text, html);
    }

    /** Welcome for an account whose temporary password was given by a manager (never sent by email). */
    public static EmailMessage welcomeWithTemporaryPassword(String to, String name, String establishment, String loginUrl) {
        String where = establishment != null ? " do estabelecimento " + establishment : "";
        String text = "Olá, " + name + "!\n\n"
                + "Foi criada uma conta" + where + " para você no Points Manager.\n"
                + "Quem cadastrou você vai informar a senha provisória. Entre em " + loginUrl + " e, no primeiro acesso, escolha a sua senha.\n";
        String html = layout("Bem-vindo ao Points Manager",
                "<p>Olá, " + escape(name) + "!</p>"
                        + "<p>Foi criada uma conta" + escape(where) + " para você no Points Manager.</p>"
                        + "<p>Quem cadastrou você vai informar a senha provisória. No primeiro acesso você escolhe a sua senha.</p>"
                        + button(loginUrl, "Ir para o login"));
        return new EmailMessage(to, "Sua conta no Points Manager", text, html);
    }

    static String humanDuration(long minutes) {
        if (minutes % 60 == 0) {
            long hours = minutes / 60;
            return hours + (hours == 1 ? " hora" : " horas");
        }
        return minutes + " minutos";
    }

    static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static String button(String url, String label) {
        return "<p><a href=\"" + escape(url) + "\" style=\"display:inline-block;padding:12px 24px;background:#6c5ce7;"
                + "color:#ffffff;text-decoration:none;border-radius:8px;font-weight:600\">" + escape(label) + "</a></p>"
                + "<p style=\"color:#666\">Se o botão não funcionar, copie este endereço no navegador:<br>" + escape(url) + "</p>";
    }

    private static String layout(String title, String body) {
        return "<div style=\"font-family:Arial,sans-serif;max-width:520px;margin:0 auto;color:#222\">"
                + "<h2 style=\"color:#6c5ce7\">" + escape(title) + "</h2>" + body + "</div>";
    }
}
