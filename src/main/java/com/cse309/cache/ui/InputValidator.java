package com.cse309.cache.ui;

import com.cse309.cache.core.CacheDefaults;

import java.time.Duration;

/**
 * Plain Java validator for JavaFX input fields.
 * Enables inline validation warnings and prevents invalid requests from reaching the engine.
 */
public final class InputValidator {

    public record ValidationResult(boolean isValid, String errorMessage) {
        public static ValidationResult valid() {
            return new ValidationResult(true, "");
        }
        public static ValidationResult error(String message) {
            return new ValidationResult(false, message);
        }
    }

    private InputValidator() {
        // Utility class
    }

    public static ValidationResult validateKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            return ValidationResult.error("Key cannot be empty or whitespace.");
        }
        if (key.length() > CacheDefaults.MAX_STRING_KEY_LENGTH) {
            return ValidationResult.error("Key length exceeds " + CacheDefaults.MAX_STRING_KEY_LENGTH + " characters.");
        }
        return ValidationResult.valid();
    }

    public static ValidationResult validateValue(String value) {
        if (value == null) {
            return ValidationResult.error("Value cannot be null.");
        }
        if (value.length() > CacheDefaults.MAX_STRING_VALUE_LENGTH) {
            return ValidationResult.error("Value length exceeds " + CacheDefaults.MAX_STRING_VALUE_LENGTH + " characters.");
        }
        return ValidationResult.valid();
    }

    public static ValidationResult validateCapacity(String text) {
        if (text == null || text.trim().isEmpty()) {
            return ValidationResult.error("Capacity is required.");
        }
        try {
            int cap = Integer.parseInt(text.trim());
            if (cap < CacheDefaults.MIN_CAPACITY || cap > CacheDefaults.MAX_CAPACITY) {
                return ValidationResult.error(String.format("Capacity must be between %d and %,d.",
                        CacheDefaults.MIN_CAPACITY, CacheDefaults.MAX_CAPACITY));
            }
            return ValidationResult.valid();
        } catch (NumberFormatException e) {
            return ValidationResult.error("Capacity must be a valid integer.");
        }
    }

    public static ValidationResult validateTtl(String text, String unit) {
        if ("Use default".equalsIgnoreCase(unit) || "Never expires".equalsIgnoreCase(unit)) {
            return ValidationResult.valid();
        }
        if (text == null || text.trim().isEmpty()) {
            return ValidationResult.error("TTL number is required for custom unit.");
        }
        try {
            long val = Long.parseLong(text.trim());
            if (val <= 0) {
                return ValidationResult.error("TTL must be greater than zero.");
            }
            Duration dur = switch (unit.toLowerCase()) {
                case "seconds" -> Duration.ofSeconds(val);
                case "minutes" -> Duration.ofMinutes(val);
                case "hours" -> Duration.ofHours(val);
                default -> Duration.ofSeconds(val);
            };
            if (dur.compareTo(CacheDefaults.MIN_TTL) < 0 || dur.compareTo(CacheDefaults.MAX_TTL) > 0) {
                return ValidationResult.error("TTL must be between 1 second and 365 days.");
            }
            return ValidationResult.valid();
        } catch (NumberFormatException e) {
            return ValidationResult.error("TTL must be a valid positive number.");
        }
    }

    public static ValidationResult validateInteger(String text, int min, int max, String fieldName) {
        if (text == null || text.trim().isEmpty()) {
            return ValidationResult.error(fieldName + " is required.");
        }
        try {
            int val = Integer.parseInt(text.trim());
            if (val < min || val > max) {
                return ValidationResult.error(String.format("%s must be between %d and %,d.", fieldName, min, max));
            }
            return ValidationResult.valid();
        } catch (NumberFormatException e) {
            return ValidationResult.error(fieldName + " must be a valid integer.");
        }
    }
}
