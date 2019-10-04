package com.tquinto.fos;

import java.util.Date;
import java.util.List;

/**
 * Represents an event that occurs to an order for these scenarios:
 * 1) order is added to a shelf
 * 2) order is picked up by a driver to be delivered
 * 3) order becomes waste after it fully decays over time
 * 4) order is manually removed as waste in order to make room on a shelf
 */
public interface OrderEvent {

    /**
     * Different types of events that can occur during order processing.
     *
     * ADDED_TO_SHELF - when the order is placed on a shelf (either when first arriving, or when being shifted around
     * to and from the overflow shelf)
     * PICKED_UP - when a driver picks up the order
     * DECAYED_WASTE - when the order has fully decayed over time and has become waste
     * REMOVED_WASTE - when the order is manually removed as waste in order to make room on a shelf
     */
    enum Type {
        ADDED_TO_SHELF,
        PICKED_UP,
        DECAYED_WASTE,
        REMOVED_WASTE
    }

    /**
     * Return order associated with this event.
     *
     * @return order associated with this event
     */
    Order getOrder();

    /**
     * Returns the type of order event.
     *
     * @return type enum value for this order event
     */
    Type getType();

    /**
     * Returns the shelf type (either temperature-related value like "hot", "cold", or "frozen") or "overflow" for the
     * specialized overflow shelf) associated with the event.
     * If the type is ADDED_TO_SHELF, this would indicate which shelf the order was added to.
     * If the type is PICKED_UP, this would indicate which shelf the driver picked up the order from.
     * If the type is WASTE_DECAYED or WASTE_REMOVED, this would indicate which shelf the order was removed from.
     * If shelf type is null, this indicates the order was thrown out before being placed on any shelf.
     *
     * @return type of shelf
     */
    String getShelfType();

    /**
     * Returns the date the event happened with the order.
     * @return date the event occurred
     */
    Date getDate();

    /**
     * Returns a list of all the shelves (and order contents) in the state when order event occurred.
     * @return list (and associated state) of all shelves when the order event occurred
     */
    List<Shelf> getShelves();
}
