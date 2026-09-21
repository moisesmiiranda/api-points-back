package com.mmiranda.pointsbackapi.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class CnpjValidator implements ConstraintValidator<Cnpj, String> {

    private static final Pattern FORMAT = Pattern.compile("^\\d{2}\\.?\\d{3}\\.?\\d{3}/?\\d{4}-?\\d{2}$");
    private static final int[] WEIGHTS_FIRST = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] WEIGHTS_SECOND = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        if (!FORMAT.matcher(value).matches()) {
            return false;
        }
        String digits = value.replaceAll("\\D", "");
        if (digits.chars().distinct().count() == 1) {
            return false;
        }
        return checkDigit(digits, WEIGHTS_FIRST) == digits.charAt(12) - '0'
                && checkDigit(digits, WEIGHTS_SECOND) == digits.charAt(13) - '0';
    }

    private static int checkDigit(String digits, int[] weights) {
        int sum = 0;
        for (int i = 0; i < weights.length; i++) {
            sum += (digits.charAt(i) - '0') * weights[i];
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }
}
