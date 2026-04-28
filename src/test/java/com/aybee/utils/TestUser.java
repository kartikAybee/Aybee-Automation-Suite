package com.aybee.utils;

public class TestUser {

    public final String email;
    public final String password;
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
