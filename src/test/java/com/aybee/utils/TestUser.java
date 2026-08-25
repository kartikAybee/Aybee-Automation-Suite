package com.aybee.utils;

public class TestUser {

    public final String email;
    // Non-final: the ForgotPassword reset scenario changes the shared account's password and
    // writes the new value back here so later reuse logs in with the current password.
    public String password;
    public final String company;
    public final String firstName;
    public final String lastName;

    public TestUser(String email, String password, String company, String firstName, String lastName) {
        this.email     = email;
        this.password  = password;
        this.company   = company;
        this.firstName = firstName;
        this.lastName  = lastName;
    }
}
