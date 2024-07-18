package com.example.accountservices.service;

public class AccountAlreadyExists extends RuntimeException {
    public AccountAlreadyExists(String s) {
        super(s);
    }
}
