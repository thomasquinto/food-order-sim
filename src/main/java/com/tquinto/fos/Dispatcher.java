package com.tquinto.fos;

/**
 * Abstraction of a dispatcher that dispatches delivery drivers who pickup food from kitchen shelves.
 */
public interface Dispatcher {

    /**
     * Returns a driver who is dispatched to pickup a particular order.
     *
     * @param order order to be picked up by this driver
     * @return driver to pickup order
     */
    Driver dispatchDriver(Order order);

}
