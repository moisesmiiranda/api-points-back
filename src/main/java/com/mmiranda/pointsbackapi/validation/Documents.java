package com.mmiranda.pointsbackapi.validation;

/** Normalization and display helpers for Brazilian CPF numbers. */
public final class Documents {

    private Documents() {
    }

    /** Only the digits of a CPF, so "529.982.247-25" and "52998224725" identify the same person. */
    public static String digits(String value) {
        return value == null ? null : value.replaceAll("\\D", "");
    }

    /** "52998224725" -> "529.982.247-25". Anything that is not 11 digits is returned unchanged. */
    public static String formatCpf(String digits) {
        if (digits == null || !digits.matches("\\d{11}")) {
            return digits;
        }
        return digits.substring(0, 3) + "." + digits.substring(3, 6) + "." + digits.substring(6, 9)
                + "-" + digits.substring(9);
    }
}
