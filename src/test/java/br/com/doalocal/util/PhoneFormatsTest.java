package br.com.doalocal.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PhoneFormatsTest {

    @Test
    void normalizesAcceptedPhoneRepresentations() {
        assertEquals("98999999999", PhoneFormats.normalizeMobile("98999999999"));
        assertEquals("98999999999", PhoneFormats.normalizeMobile("(98) 99999-9999"));
        assertEquals("98999999999", PhoneFormats.normalizeMobile("98 99999-9999"));
    }

    @Test
    void rejectsInvalidOrIncompletePhones() {
        assertNull(PhoneFormats.normalizeMobile(null));
        assertNull(PhoneFormats.normalizeMobile(""));
        assertNull(PhoneFormats.normalizeMobile("abc"));
        assertNull(PhoneFormats.normalizeMobile("123"));
        assertNull(PhoneFormats.normalizeMobile("(98) 999"));
        assertNull(PhoneFormats.normalizeMobile("(98) abcde-9999"));
    }

    @Test
    void formatsWithoutDuplicatingTheMask() {
        assertEquals("(98) 99999-9999", PhoneFormats.formatForDisplay("98999999999"));
        assertEquals("(98) 99999-9999", PhoneFormats.formatForDisplay("(98) 99999-9999"));
    }
}
