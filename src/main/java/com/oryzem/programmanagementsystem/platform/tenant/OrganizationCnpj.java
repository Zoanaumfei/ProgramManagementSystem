package com.oryzem.programmanagementsystem.platform.tenant;

final class OrganizationCnpj {

    private OrganizationCnpj() {
    }

    static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String digits = value.replaceAll("\\D", "");
        if (digits.isBlank()) {
            return null;
        }
        if (digits.length() != 14) {
            throw new IllegalArgumentException("Organization CNPJ must contain 14 digits.");
        }
        if (allDigitsEqual(digits) || !hasValidCheckDigits(digits)) {
            throw new IllegalArgumentException("Organization CNPJ is invalid.");
        }
        return digits;
    }

    private static boolean allDigitsEqual(String digits) {
        char first = digits.charAt(0);
        for (int index = 1; index < digits.length(); index++) {
            if (digits.charAt(index) != first) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasValidCheckDigits(String digits) {
        return calculateCheckDigit(digits, 12) == Character.digit(digits.charAt(12), 10)
                && calculateCheckDigit(digits, 13) == Character.digit(digits.charAt(13), 10);
    }

    private static int calculateCheckDigit(String digits, int baseLength) {
        int[] weights = baseLength == 12
                ? new int[] {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2}
                : new int[] {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int sum = 0;
        for (int index = 0; index < baseLength; index++) {
            sum += Character.digit(digits.charAt(index), 10) * weights[index];
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }
}
