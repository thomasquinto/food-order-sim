package com.tquinto.fos.basic;

import com.tquinto.fos.DecayFormula;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for BasicDecayFormula.
 */
public class BasicDecayFormulaTest {

    /**
     * Test actual calculation of decay value.
     */
    @Test
    public void testDecayValue() {
        DecayFormula formula = new BasicDecayFormula();
        float value = formula.getDecayValue(20, .63f, 1);
        assertEquals(value, (20 - 1 - .63f));
    }

    /**
     * Test actual calculation of decay duration.
     */
    @Test
    public void testDecayDuration() {
        DecayFormula formula = new BasicDecayFormula();
        float value = formula.getDecayDuration(20, .63f);
        assertEquals(value, 20 / (1 + .63f));
    }
}
