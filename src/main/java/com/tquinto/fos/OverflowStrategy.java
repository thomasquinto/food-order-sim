package com.tquinto.fos;

import org.javatuples.Pair;

import java.util.Date;

/**
 * Interface for an overflow strategy for a kitchen which dictates the logic of moving orders to and from the overflow
 * shelf.
 */
public interface OverflowStrategy {

    /**
     * Invoked by a kitchen object when an order is received and its designated temperature shelf is full, such that
     * either the incoming order or an order on the shelf of the designated temperature (of the incoming order) needs
     * to be moved to the overflow shelf.
     *
     * @param kitchen kitchen object
     * @param incomingOrder incoming order just received by the kitchen before being placed on a shelf
     * @param date date at which the incoming order is received by the kitchen
     * @return order that will be put on the overflow shelf
     */
    public Order onTempShelfFull(Kitchen kitchen, Order incomingOrder, Date date);

    /**
     * Invoked by a kitchen object when an order of a certain temperature ("hot", "cold", or "frozen") is full and
     * the overflow shelf is also full, such that an order needs to be removed.
     *
     * @param kitchen Kitchen object
     * @param incomingOrder incoming order (order was just placed and not yet on a shelf)
     * @param date date when incoming order is received
     * @return Pair object with two orders: 1) order to remove, 2) order to replace vacated spot of removed item
     */
    Pair<Order, Order> onOverflowShelfFull(Kitchen kitchen, Order incomingOrder, Date date);


    /**
     * Invoked by a kitchen object when an order either expired (fully decayed) or was picked up by a driver.
     * This gives the strategy the opportunity to move an order from the overflow shelf to replace the removed order.
     *
     * @param kitchen kitchen containing shelves and orders
     * @param removedOrder order that was just removed from a shelf
     * @param date date when order was removed from shelf
     */
    Order onOrderRemoved(Kitchen kitchen, Order removedOrder, Date date);

}
