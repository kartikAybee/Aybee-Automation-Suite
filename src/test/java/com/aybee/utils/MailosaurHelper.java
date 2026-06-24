package com.aybee.utils;

import com.mailosaur.MailosaurClient;
import com.mailosaur.MailosaurException;
import com.mailosaur.models.Code;
import com.mailosaur.models.Message;
import com.mailosaur.models.MessageSearchParams;
import com.mailosaur.models.SearchCriteria;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MailosaurHelper {

    private static final String API_KEY   = ConfigReader.get("MAILOSAUR_API_KEY");
    private static final String SERVER_ID = ConfigReader.get("MAILOSAUR_SERVER_ID");

    // Base36 full timestamp — ~8 chars, never cycles, stays under 35 chars total.
    public static String generateEmail() {
        String email = "a" + Long.toString(System.currentTimeMillis(), 36) + "@" + SERVER_ID + ".mailosaur.net";
        System.out.println("[Mailosaur] Generated test email: " + email);
        return email;
    }

    public Message waitForEmail(String emailId) {
        try {
            MailosaurClient mailosaur = new MailosaurClient(API_KEY);
            MessageSearchParams params = new MessageSearchParams();
            params.withServer(SERVER_ID);
            params.withTimeout(60000);
            SearchCriteria criteria = new SearchCriteria();
            criteria.withSentTo(emailId);
            System.out.println("[Mailosaur] Waiting for email to: " + emailId);
            Message message = mailosaur.messages().get(params, criteria);
            System.out.println("[Mailosaur] Received — subject: " + message.subject());
            return message;
        } catch (MailosaurException | IOException e) {
            throw new RuntimeException("[Mailosaur] No email for: " + emailId, e);
        }
    }

    // Three-layer extraction so codes in hidden or non-standard HTML elements are caught.
    public String getOtpForEmail(String email) {
        Message message = waitForEmail(email);
        List<Code> codes = message.html() != null ? message.html().codes() : null;
        if (codes != null && !codes.isEmpty()) return codes.get(0).value();
        codes = message.text() != null ? message.text().codes() : null;
        if (codes != null && !codes.isEmpty()) return codes.get(0).value();
        Matcher m = Pattern.compile("\\b(\\d{6})\\b").matcher(rawBody(message));
        if (m.find()) return m.group(1);
        throw new RuntimeException("[Mailosaur] No verification code found for: " + email);
    }

    private String rawBody(Message message) {
        if (message.html() != null && message.html().body() != null) return message.html().body();
        if (message.text() != null && message.text().body() != null) return message.text().body();
        return "";
    }
}
