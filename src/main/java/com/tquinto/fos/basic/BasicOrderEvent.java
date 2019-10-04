package com.tquinto.fos.basic;

import com.tquinto.fos.Order;
import com.tquinto.fos.OrderEvent;
import com.tquinto.fos.Shelf;

import java.util.Date;
import java.util.List;

/**
 * Represents an event that occurs to an order for these scenarios:
 * 1) order is added to a shelf
 * 2) order is picked up by a driver to be delivered
 * 3) order becomes waste after it fully decays over time
 * 4) order is manually removed as waste in order to make room on a shelf
 */
public class BasicOrderEvent implements OrderEvent {

    private final Order order;
    private final OrderEvent.Type type;
    private final String shelfType;
    private final Date date;
    private final List<Shelf> shelves;

    /**
     * Constructor for a BasicOrderEvent.
     * @param order order associated with event
     * @param type order event type (i.e. ADDED_TO_SHELF, PICKED_UP, etc.)
     * @param shelfType type of shelf the order was added to or removed from (can be null in the case an incoming order
     *                  is immediately thrown out as waste and never added to a shelf)
     * @param date date at which event occurred
     * @param shelves list of all the shelves of the kitchen at the time of event occurrence
     */
    public BasicOrderEvent(Order order, OrderEvent.Type type, String shelfType, Date date, List<Shelf> shelves) {
        this.order = order;
        this.type  = type;
        this.shelfType = shelfType;
        this.date = date;
        this.shelves = shelves;
    }

    /**
     * Return order associated with this event.
     *
     * @return order associated with this event
     */
    public Order getOrder() {
        return order;
    }

    /**
     * Returns the type of order event.
     *
     * @return type enum value for this order event
     */
    public OrderEvent.Type getType() {
        return type;
    }

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
    public String getShelfType() {
        return shelfType;
    }

    /**
     * Returns the date the event happened with the order.
     *
     * @return date the event occurred
     */
    public Date getDate() {
        return date;
    }

    /**
     * Returns a list of all the shelves (and order contents) in the state when order event occurred.
     *
     * @return list (and associated state) of all shelves when the order event occurred
     */
    public List<Shelf> getShelves() {
        return shelves;
    }

}
