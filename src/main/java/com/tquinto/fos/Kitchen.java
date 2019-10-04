package com.tquinto.fos;

import io.reactivex.Observable;

import java.util.Set;

/**
 * Abstraction for a kitchen that receives orders, puts the orders on a shelf, and dispatches a drivers to pickup the
 * orders.
 *
 * Kitchens have two types of shelves: temperature-specific shelves, such as "hot", "cold" or "frozen", which can only
 * hold food orders at their designated temperature type, and the "overflow" shelf that can only be used when a
 * temperature-specific shelf becomes full. The overflow shelf can hold food orders of any temperature type, but the
 * overflow shelf may increase the decay rate of orders placed on it. When shelves free up, overflow orders can be
 * placed back on their temperature-specific shelves.
 *
 * When an order is placed in the kitchen (or a new order arrives in the order stream) and added to a shelf, a driver
 * is also dispatched to pickup the order. If the driver arrives before the order fully decays over time (based on a
 * decay formula), the driver can successfully deliver the food. Orders that fully decay over time are thrown out as
 * waste. Orders that don't fully decay also can be thrown out as waste if the kitchen's shelves are full and room needs
 * to be made for incoming orders.
 *
 * Overflow strategies provide the logic of what the kitchen should do in the cases of:
 * 1) an order arrives and all shelves are full
 * 2) an order is removed (either by driver pickup or when an order has fully decayed to waste)
 */
public interface Kitchen {

    /**
     * Returns a set of all temperature-specific shelf types (i.e. "hot", "cold", or "frozen").
     * This does not include the "overflow" shelf.
     *
     * @return set of all shelf temperature types
     */
    Set<String> getShelfTemps();

    /**
     * Returns a shelf for a given temperature, such as "hot", "cold", or "frozen".
     * This does not include the "overflow" shelf.
     *
     * @param temp temperature type of shelf
     * @return shelf of the requested temperature type
     */
    Shelf getShelf(String temp);

    /**
     * Returns the overflow shelf, where orders can be placed when a certain temperature-specific shelf is full.
     * The overflow shelf can hold orders of any temperature ("hot", "cold", or "frozen").
     *
     * @return overflow shelf
     */
    Shelf getOverflowShelf();

    /**
     * A kitchen processes orders from an order source, which will provide a stream of orders.
     *
     * @param orderSource source of orders
     * @return observable that emits a stream of resulting order events that chronicles when orders are added to
     * shelves, picked up by drivers, decay to waste, or are manually removed as waste
     */
    Observable<OrderEvent> processOrders(OrderSource orderSource);

}
