package com.mmiranda.pointsbackapi.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentValidatorsTest {

    private final CpfValidator cpf = new CpfValidator();
    private final CnpjValidator cnpj = new CnpjValidator();

    @Test
    void cpfAcceptsValidNumbersWithAndWithoutPunctuation() {
        assertTrue(cpf.isValid("529.982.247-25", null));
        assertTrue(cpf.isValid("52998224725", null));
        assertTrue(cpf.isValid("111.444.777-35", null));
    }

    @Test
    void cpfTreatsNullAsValidSoOptionalFieldsPass() {
        assertTrue(cpf.isValid(null, null));
    }

    @Test
    void cpfRejectsWrongCheckDigitsRepeatedDigitsAndBadFormat() {
        assertFalse(cpf.isValid("529.982.247-24", null));
        assertFalse(cpf.isValid("111.111.111-11", null));
        assertFalse(cpf.isValid("123", null));
        assertFalse(cpf.isValid("529.982.247-2a", null));
        assertFalse(cpf.isValid("", null));
    }

    @Test
    void cpfHandlesCheckDigitRemainderOfTen() {
        // A remainder of 10 must yield a check digit of 0 (first digit, then second digit)
        assertTrue(cpf.isValid("100.000.001-08", null));
        assertTrue(cpf.isValid("100.000.028-10", null));
    }

    @Test
    void cnpjAcceptsValidNumbersWithAndWithoutPunctuation() {
        assertTrue(cnpj.isValid("11.222.333/0001-81", null));
        assertTrue(cnpj.isValid("11222333000181", null));
        assertTrue(cnpj.isValid("11.444.777/0001-61", null));
    }

    @Test
    void cnpjTreatsNullAsValid() {
        assertTrue(cnpj.isValid(null, null));
    }

    @Test
    void cnpjRejectsWrongCheckDigitsRepeatedDigitsAndBadFormat() {
        assertFalse(cnpj.isValid("11.222.333/0001-82", null));
        assertFalse(cnpj.isValid("11.111.111/1111-11", null));
        assertFalse(cnpj.isValid("123", null));
        assertFalse(cnpj.isValid("11.222.333/0001-8x", null));
    }
}
