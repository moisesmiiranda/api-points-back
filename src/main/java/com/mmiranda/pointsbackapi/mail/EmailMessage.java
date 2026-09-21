package com.mmiranda.pointsbackapi.mail;

/** An outgoing email: plain-text body plus an HTML alternative. */
public record EmailMessage(String to, String subject, String text, String html) {
}
