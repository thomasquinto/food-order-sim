package com.tquinto.fos.basic;

import com.tquinto.fos.DecayFormula;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for BasicDecayFormula.
 */
public class BasicDecayFormulaTest {

    /**
     * Test actual values for decay formula.
     */
    @Test
    public void testDecayValue() {
        DecayFormula formula = new BasicDecayFormula();
        float value = formula.getDecayValue(20, .63f, 1);
        assertEquals(value, (20 - 1 - .63f));
    }

    /**
     * Test actual values for normalized decay formula.
     */
    @Test
    public void testNormalizedDecayValue() {
        DecayFormula formula = new BasicDecayFormula();
        float value = formula.getDecayValue(20, .63f, 1);
        assertEquals(value, (20 - 1 - .63f) / 20);
    }
}
