package com.tquinto.fos.basic;

import com.tquinto.fos.DecayFormula;

/**
 * Implementation for a decay formula, which calculates a decay value based on shelf life, current order
 * age (how long an order has already been on a shelf) and decay rate.
 *
 * This particular implementation uses the following decay formula:
 * [decay value] = ( [shelf life] - [order age] ) - ( [decay rate] * [order age] )
 */
public class BasicDecayFormula implements DecayFormula {

    /**
     * Using the implemented decay formula, calculates decay value using shelf life, decay rate and order age.
     *
     * @param shelfLife shelf life of order
     * @param decayRate decay rate of order
     * @param orderAge age of order, or how long the order has been on a shelf (in the same units as shelf life)
     * @return decay value based on decay formula with the three parameters as input
     */
    @Override
    public float getDecayValue(int shelfLife, float decayRate, int orderAge) {
        return Math.max((shelfLife - (orderAge * (1 + decayRate))), 0f);
    }

    /**
     * Using the implemented decay formula, calculates the total duration of time until the decay value reaches the
     * value of 0, meaning the order has fully decayed to waste.
     *
     * @param shelfLife shelf life of order
     * @param decayRate decay rate of order
     * @return time until order will reach a decay value of zero
     */
    @Override
    public float getDecayDuration(int shelfLife, float decayRate) {
        return shelfLife / (1 + decayRate);
    }

}
