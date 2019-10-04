package com.tquinto.fos;

import java.util.concurrent.TimeUnit;

/**
 * Abstraction for a driver that is dispatched to pickup a particular order, as soon as the order is placed
 */
public interface Driver {

    /**
     * Time unit value for drive duration (such as SECONDS).
     *
     * @return time unit value for drive duration (such as SECONDS)
     */
    TimeUnit getTimeUnit();

    /**
     * Duration in time units for the driver to arrive to pickup order.
     *
     * @return duration in time units for the driver to arrive to pickup order
     */
    int getDriveDuration();

    /**
     * Order to be picked up by driver.
     *
     * @return order to be picked up by driver
     */
    Order getOrder();

}
