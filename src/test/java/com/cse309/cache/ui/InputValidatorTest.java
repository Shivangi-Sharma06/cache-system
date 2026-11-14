package com.cse309.cache.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InputValidatorTest {

    @Test
    void testValidateKey() {
        assertTrue(InputValidator.validateKey("validKey").isValid());
        assertFalse(InputValidator.validateKey("").isValid());
        assertFalse(InputValidator.validateKey("   ").isValid());
        assertFalse(InputValidator.validateKey("a".repeat(251)).isValid());
        assertTrue(InputValidator.validateKey("a".repeat(250)).isValid());
    }

    @Test
    void testValidateValue() {
        assertTrue(InputValidator.validateValue("hello").isValid());
        assertFalse(InputValidator.validateValue(null).isValid());
        assertFalse(InputValidator.validateValue("x".repeat(8193)).isValid());
        assertTrue(InputValidator.validateValue("x".repeat(8192)).isValid());
    }

    @Test
    void testValidateCapacity() {
        assertTrue(InputValidator.validateCapacity("100").isValid());
        assertTrue(InputValidator.validateCapacity("1").isValid());
        assertTrue(InputValidator.validateCapacity("1000000").isValid());
        assertFalse(InputValidator.validateCapacity("0").isValid());
        assertFalse(InputValidator.validateCapacity("1000001").isValid());
        assertFalse(InputValidator.validateCapacity("abc").isValid());
    }

    @Test
    void testValidateTtl() {
        assertTrue(InputValidator.validateTtl("", "Use default").isValid());
        assertTrue(InputValidator.validateTtl("", "Never expires").isValid());
        assertTrue(InputValidator.validateTtl("60", "Seconds").isValid());
        assertFalse(InputValidator.validateTtl("0", "Seconds").isValid());
        assertFalse(InputValidator.validateTtl("-5", "Seconds").isValid());
        assertFalse(InputValidator.validateTtl("abc", "Seconds").isValid());
    }
}
