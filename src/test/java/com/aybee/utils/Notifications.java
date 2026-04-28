package com.aybee.utils;

public class Notifications {

    // ─── Sign up ──────────────────────────────────────────────────────────────────
    public static final String EMAIL_ALREADY_REGISTERED =
            "This email is already registered. Please log in to your account instead.";

    // ─── Sign in ──────────────────────────────────────────────────────────────────
    public static final String INVALID_SIGNIN_CREDENTIALS =
            "Invalid email or password. Please try again or reset your password.";

    // ─── OTP activation page ─────────────────────────────────────────────────────
    public static final String ACTIVATION_CODE_RESENT =
            "A new security code has been sent";

    public static final String INVALID_OTP =
            "The security code is invalid or expired";

    // ─── Reset password page ──────────────────────────────────────────────────────
    public static final String PASSWORDS_DO_NOT_MATCH =
            "The two passwords do not match!";

    // ─── Team invite ──────────────────────────────────────────────────────────────
    // Toast shown after clicking the Copy Link button in the invite sidebar
    public static final String INVITE_LINK_COPIED = "Your link was copied";

    // Toast shown after sending an email invite — contains the invitee email and role
    // e.g. "You have invited alice@example.com as a Creator"
    // Assert with: actual.contains("You have invited") && actual.contains(inviteeEmail)
    public static final String INVITE_EMAIL_SENT_PREFIX = "You have invited";

    // ─── Google auth (fill in when Google login is re-enabled) ───────────────────
    public static final String GOOGLE_SIGNUP_BLOCKED_EMAIL_ACCOUNT =
            "TODO: update from Jam recording bda1d2a7";

    public static final String GOOGLE_SIGNIN_BLOCKED_EMAIL_ACCOUNT =
            "TODO: update from Jam recording 2ab06bb6";

    public static final String MANUAL_SIGNUP_BLOCKED_GOOGLE_ACCOUNT =
            "TODO: update from Jam recording f4137984";

    public static final String GOOGLE_SIGNUP_BLOCKED_GOOGLE_ACCOUNT =
            "TODO: update from Jam recording 83315274";

    public static final String MANUAL_SIGNIN_BLOCKED_GOOGLE_ACCOUNT =
            "TODO: update from Jam recording 5db755ea";

    public static final String TEAM_INVITE_EMAIL_ALREADY_EXISTS =
            "TODO: update from Jam recording 2803fe53";

    public static final String PERSONAL_INVITE_GOOGLE_BLOCKED =
            "TODO: update from Jam recording b6f18169";
}
