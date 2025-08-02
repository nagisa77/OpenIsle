package com.openisle.service;

/**
 * Abstract email sender used to deliver emails.
 */
public abstract class EmailSender {
    /**
     * Send an email to a recipient.
     * @param to recipient email address
     * @param subject email subject
     * @param text email body
     */
    public abstract void sendEmail(String to, String subject, String text);
}
