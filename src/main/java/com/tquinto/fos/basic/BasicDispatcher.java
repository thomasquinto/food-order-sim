package com.tquinto.fos.basic;

import com.tquinto.fos.Dispatcher;
import com.tquinto.fos.Driver;
import com.tquinto.fos.Order;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Abstraction of a dispatcher that dispatches delivery drivers who pickup food from kitchen shelves.
 *
 * This dispatcher implementation dispatches drivers that will take a random duration of time to arrive to pickup a food
 * order within a specified range.
 */
public class BasicDispatcher implements Dispatcher {

    private final TimeUnit timeUnit;
    private int minDriveDuration;
    private int maxDriveDuration;

    /**
     * Constructor for a basic dispatcher.
     * Any driver dispatched will be be bounded my a minimum and maximum drive duration, which is the time it takes for
     * a driver to pickup an order. The driver is dispatched the instant the food order is placed.
     *
     * @param timeUnit unit of time (e.g. SECONDS)
     * @param minDriveDuration minimum drive duration for drivers to arrive to pickup an order
     * @param maxDriveDuration maximum drive duration for drivers to arrive to pickup an order
     */
    public BasicDispatcher(TimeUnit timeUnit, int minDriveDuration, int maxDriveDuration) {
        this.timeUnit = timeUnit;
        this.minDriveDuration = minDriveDuration;
        this.maxDriveDuration = maxDriveDuration;
    }

    /**
     * Returns a driver who is dispatched to pickup a particular order.
     *
     * @param order order to be picked up by this driver
     * @return driver to pickup order
     */
    @Override
    public Driver dispatchDriver(Order order) {
        return new BasicDriver(timeUnit, randomNumberBetween(minDriveDuration, maxDriveDuration), order);
    }

    /**
     * Generate a random number between low and high, inclusive
     *
     * @param low lowest possible random number
     * @param high highest possible random number
     * @return random number between low and high, inclusive
     */
    private int randomNumberBetween(int low, int high) {
        return new Random().nextInt(high + 1 - low) + low;
    }

}
