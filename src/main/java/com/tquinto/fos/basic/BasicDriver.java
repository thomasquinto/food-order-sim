package com.tquinto.fos.basic;

import com.tquinto.fos.Driver;
import com.tquinto.fos.Order;

import java.util.concurrent.TimeUnit;

/**
 * Abstraction for a driver that is dispatched to pickup a particular order, as soon as the order is placed
 */
public class BasicDriver implements Driver {

    private final TimeUnit timeUnit;
    private final int driveDuration;
    private final Order order;

    /**
     * Constructor for a driver.
     *
     * @param timeUnit time unit value for drive duration (such as SECONDS)
     * @param driveDuration duration in time units for the driver to arrive to pickup order
     * @param order order to be picked up by driver
     */
    public BasicDriver(TimeUnit timeUnit, int driveDuration, Order order) {
        this.timeUnit = timeUnit;
        this.driveDuration = driveDuration;
        this.order = order;
    }

    /**
     * Time unit value for drive duration (such as SECONDS).
     *
     * @return time unit value for drive duration (such as SECONDS)
     */
    @Override
    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    /**
     * Duration in time units for the driver to arrive to pickup order.
     *
     * @return duration in time units for the driver to arrive to pickup order
     */
    @Override
    public int getDriveDuration() {
        return driveDuration;
    }

    /**
     * Order to be picked up by driver.
     *
     * @return order to be picked up by driver
     */
    @Override
    public Order getOrder() {
        return order;
    }

}
