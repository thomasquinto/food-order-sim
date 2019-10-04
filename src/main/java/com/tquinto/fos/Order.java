package com.tquinto.fos;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * An abstraction for a food order that is placed with a kitchen, is added to a shelf in the kitchen, and then picked up
 * by a driver dispatched to deliver the order. The order decays over time after being added to a shelf which is
 * calculated by a decay formula. Fully decayed orders are thrown out as waste.
 */
public interface Order extends Cloneable {

    /**
     * Enforces that <code>initialize()</code> method must be invoked before accessing some functions that require
     * initialized internal state to perform time-based decay calculations.
     */
    class NotInitializedException extends RuntimeException {
        public NotInitializedException(String errorMessage) {
            super(errorMessage);
        }
    }

    /**
     * A unique identifier for the order. No two distinct orders will have the same id.
     *
     * @return unique id of order
     */
    long getId();

    /**
     * Name field of the order.
     *
     * @return name of order
     */
    String getName();

    /**
     * Temperature category of the order, i.e. "hot", "cold", or "frozen"
     *
     * @return temperature value of order
     */
    String getTemp();

    /**
     * Shelf life of order. Used by the food order decay formula to determine when an order becomes waste after some
     * duration of time.
     *
     * @return shelf life of order
     */
    int getShelfLife();

    /**
     * Decay rate of order. Used by the food order decay formula to calculate when an order becomes waste after some
     * duration of time.
     *
     * @return decay rate of order used by decay formula
     */
    float getDecayRate();

    /**
     * Used to initialize the internal state of an order when the order is placed. This must be called before retrieving
     * the decay value, normalized decay value and decay duration. Invoked by kitchen when the order is first placed.
     *
     * @param birthDate date the order was added to a shelf
     */
    void initialize(Date birthDate);

    /**
     * Time unit of order, such as SECONDS. Applies to shelf life and decay duration.
     *
     * @return unit of time for time fields for orders (shelf life and decay duration)
     */
    TimeUnit getTimeUnit();

    /**
     * Birth date of order, or when the order is originally placed with the kitchen.
     *
     * @return date the order was placed
     */
    Date getBirthDate();

    /**
     * Returns the current decay rate of the order based on the current shelf it was added to.
     *
     * @return current decay rate of order based on the current shelf it was added to
     */
    float getCurrentDecayRate();

    /**
     * Returns the date the order was added to its current shelf.
     *
     * @return date the order was added to its current shelf
     */
    Date getAddedToShelfDate();

    /**
     * Since an order can move between shelves over its lifetime, the adjusted shelf life takes into account how
     * long the order has already aged on previous shelves (if any) since it was initialized. The adjusted shelf life
     * uses <code></code>getAddedToShelfDate()</code> as its time reference point.
     *
     * @return adjusted shelf life
     */
    int getAdjustedShelfLife();

    /**
     * Returns decay value as calculated by the food order decay formula.
     *
     * @param currentDate date from which to calculate order age with respect to order birth date
     * @return decay value as calculated by the food order decay formula
     */
    float getDecayValue(Date currentDate);

    /**
     * Returns normalized decay value as calculated by the food order decay formula, which is the decay value divided
     * by the shelf life.
     *
     * @param currentDate current date
     * @return normalized decay value
     */
    float getNormalizedDecayValue(Date currentDate);

    /**
     * Returns decay duration, which is the number of time units remaining before the decay value reaches a value of
     * zero.
     *
     * @param currentDate current date
     * @return decay duration in time units
     */
    float getDecayDuration(Date currentDate);

    /**
     * Since shelves can have affect the decay rate (e.g. orders on the overflow shelf decay at twice the rate than
     * normal temperature-specific shelves), this method is invoked at the time the order is placed on a shelf.
     *
     * @param currentDate date at which order is placed on a shelf with a given decay rate
     * @param decayRate new decay rate for order, which is usually a multiple of the order's original decay rate
     */
    void updateDecayRate(Date currentDate, float decayRate);

    /**
     * Public version of the clone method for the Cloneable interface.
     *
     * @return cloned order object
     * @throws CloneNotSupportedException
     */
    Object clone() throws CloneNotSupportedException;

}
