package com.example.accountservices.entity;

import java.time.Year;

public class AccountUtils {

    public static final String ACCOUNT_ALREADY_EXISTS = "Account already exists";

    public static String generateAccountNumber() {
        Year currentYear = Year.now();
        int min=10000000;
        int max=99999999;
        int randomNum= (int) Math.floor(Math.random() *(max-min+1)+min);

        String year=String.valueOf(currentYear);
        String randomNumber=String.valueOf(randomNum);

        return year + randomNumber;
    }
}
