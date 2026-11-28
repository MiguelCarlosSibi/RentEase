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

    /** Converts 09XXXXXXXXX to 639XXXXXXXXX, the format used when sending through Semaphore (FR055). */
    public static String toInternational(String mobile) {
        String value = normalize(mobile);
        if (value.startsWith("09")) {
            return "63" + value.substring(1);
        }
        return value;
    }
}
