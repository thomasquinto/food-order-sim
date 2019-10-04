package com.tquinto.fos;

/**
 * Interface that encapsulates the decay formula, which calculates a decay value based on shelf life, current order
 * age (how long an order has already been on a shelf) and decay rate.
 */
public interface DecayFormula {

    /**
     * Using the implemented decay formula, calculates decay value using shelf life, decay rate and order age.
     *
     * @param shelfLife shelf life of order
     * @param decayRate decay rate of order
     * @param orderAge age of order, or how long the order has been on a shelf (in the same units as shelf life)
     * @return decay value based on decay formula with the three parameters as input
     */
    float getDecayValue(int shelfLife, float decayRate, int orderAge);

    /**
     * Using the implemented decay formula, calculates the total duration of time until the decay value reaches the
     * value of 0, meaning the order has fully decayed to waste.
     *
     * @param shelfLife shelf life of order
     * @param decayRate decay rate of order
     * @return time until order will reach a decay value of zero
     */
    float getDecayDuration(int shelfLife, float decayRate);
}
