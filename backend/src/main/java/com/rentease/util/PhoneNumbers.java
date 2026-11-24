package com.rentease.util;

public final class PhoneNumbers {

    private PhoneNumbers() {
    }

    /** Stores Philippine mobile numbers in one format: 09XXXXXXXXX. */
    public static String normalize(String mobile) {
        String value = mobile.trim();
        if (value.startsWith("+63")) {
            return "0" + value.substring(3);
        }
        return value;
    }
}
