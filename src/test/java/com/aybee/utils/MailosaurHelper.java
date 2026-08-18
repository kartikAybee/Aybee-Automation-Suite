package com.aybee.utils;

import com.mailosaur.MailosaurClient;
import com.mailosaur.MailosaurException;
import com.mailosaur.models.Code;
import com.mailosaur.models.Link;
import com.mailosaur.models.Message;
import com.mailosaur.models.MessageListParams;
import com.mailosaur.models.MessageListResult;
import com.mailosaur.models.MessageSearchParams;
import com.mailosaur.models.MessageSummary;
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
            dumpInbox();
            throw new RuntimeException(
                    "[Mailosaur] No email for: " + emailId + " — inbox dump above shows what arrived.", e);
        }
    }

    public Message waitForEmailWithSubject(String emailId, String subject) {
        try {
            MailosaurClient mailosaur = new MailosaurClient(API_KEY);
            MessageSearchParams params = new MessageSearchParams();
            params.withServer(SERVER_ID);
            params.withTimeout(60000);
            SearchCriteria criteria = new SearchCriteria();
            criteria.withSentTo(emailId);
            criteria.withSubject(subject);
            System.out.println("[Mailosaur] Waiting for email to: " + emailId + " with subject: " + subject);
            Message message = mailosaur.messages().get(params, criteria);
            System.out.println("[Mailosaur] Received — subject: " + message.subject());
            return message;
        } catch (MailosaurException | IOException e) {
            dumpInbox();
            throw new RuntimeException(
                    "[Mailosaur] No email for: " + emailId + " with subject: " + subject + " — inbox dump above.", e);
        }
    }

    // Three-layer extraction so codes in hidden or non-standard HTML elements are caught.
    public String getOtpForEmail(String email) {
        return extractOtp(waitForEmail(email), email);
    }

    // Subject-filtered variant — use when other emails for the same address may already
    // be in the inbox (e.g. an invite email arrived before the OTP was requested).
    public String getOtpForEmailWithSubject(String email, String subjectFilter) {
        return extractOtp(waitForEmailWithSubject(email, subjectFilter), email);
    }

    private String extractOtp(Message message, String email) {
        List<Code> codes = message.html() != null ? message.html().codes() : null;
        if (codes != null && !codes.isEmpty()) return codes.get(0).value();
        codes = message.text() != null ? message.text().codes() : null;
        if (codes != null && !codes.isEmpty()) return codes.get(0).value();
        Matcher m = Pattern.compile("\\b(\\d{6})\\b").matcher(rawBody(message));
        if (m.find()) return m.group(1);
        throw new RuntimeException("[Mailosaur] No verification code found for: " + email
                + "\nHTML body: " + (message.html() != null ? message.html().body() : "null"));
    }

    public String getInviteUrlForEmail(String email) {
        System.out.println("[Mailosaur] Fetching invite email for: " + email);
        Message message = waitForEmail(email);
        System.out.println("[Mailosaur] Invite email received — subject: " + message.subject());
        List<Link> links = message.html() != null ? message.html().links() : null;
        if (links != null) {
            for (Link link : links) {
                if (link.text() != null && link.text().toLowerCase().contains("accept invitation")) {
                    String url = decode(link.href());
                    System.out.println("[Mailosaur] Invite URL (text match): " + url);
                    return url;
                }
            }
            for (Link link : links) {
                if (link.href() != null && link.href().toLowerCase().contains("invitation")) {
                    String url = decode(link.href());
                    System.out.println("[Mailosaur] Invite URL (href match): " + url);
                    return url;
                }
            }
        }
        Matcher m = Pattern.compile(
                "href=[\"'](https?://[^\"']*invitation[^\"']*)[\"']",
                Pattern.CASE_INSENSITIVE).matcher(rawBody(message));
        if (m.find()) {
            String url = decode(m.group(1));
            System.out.println("[Mailosaur] Invite URL (regex match): " + url);
            return url;
        }
        throw new RuntimeException("[Mailosaur] No 'Accept invitation' link found for: " + email
                + "\nSubject: " + message.subject()
                + "\nHTML body: " + (message.html() != null ? message.html().body() : "null"));
    }

    // Dumps every message in the inbox — called on failure so the log reveals what arrived.
    public void dumpInbox() {
        try {
            MailosaurClient client = new MailosaurClient(API_KEY);
            MessageListParams params = new MessageListParams().withServer(SERVER_ID);
            MessageListResult result = client.messages().list(params);
            List<MessageSummary> items = result.items();
            if (items == null || items.isEmpty()) {
                System.out.println("[Mailosaur] Inbox is empty — Aybee sent no emails at all.");
            } else {
                System.out.println("[Mailosaur] Inbox has " + items.size() + " message(s):");
                for (MessageSummary ms : items) {
                    String to = (ms.to() != null && !ms.to().isEmpty())
                            ? ms.to().get(0).email() : "unknown";
                    System.out.println("  subject=" + ms.subject() + "  to=" + to
                            + "  received=" + ms.received());
                }
            }
        } catch (Exception ex) {
            System.out.println("[Mailosaur] Could not dump inbox: " + ex.getMessage());
        }
    }

    private String rawBody(Message message) {
        if (message.html() != null && message.html().body() != null) return message.html().body();
        if (message.text() != null && message.text().body() != null) return message.text().body();
        return "";
    }

    private String decode(String href) {
        if (href == null) return "";
        return href.replace("&amp;", "&");
    }
}
