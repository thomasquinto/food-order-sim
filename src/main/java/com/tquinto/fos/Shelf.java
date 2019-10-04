package com.tquinto.fos;

import java.util.*;

/**
 * An abstraction for a shelf in a kitchen which holds orders to be picked up by a driver.
 */
public interface Shelf extends Cloneable {

    /**
     * This exception type is thrown if an order is added of some other type not included in
     * <code>getAcceptedTypes()</code>.
     */
    class InvalidOrderTypeException extends RuntimeException {
        public InvalidOrderTypeException(String errorMessage) {
            super(errorMessage);
        }
    }

    /**
     * Type of shelf.
     *
     * For "temperature" shelves, orders have the same "temp" value as the shelf's type.
     *
     * @return String value of shelf type (i.e. "hot", "cold", "frozen", "overflow")
     */
    String getType();

    /**
     * Returns string array of the types of orders this shelf accepts.
     *
     * @return String array of accepted order types.
     */
    Set<String> getAcceptedTypes();

    /**
     * Sets the accepted order types for this shelf. If an order is added for any other type, then an
     * <code>InvalidOrderTypeException</code> will be thrown.
     * @param acceptedTypes
     */
    void setAcceptedTypes(Set<String> acceptedTypes);

    /**
     * Returns limit of number orders that can be added to this shelf.
     *
     * @return limit of number orders that can be added to this shelf
     */
    int getLimit();

    /**
     * Multiplier applied to decay rates of orders added to this shelf.
     *
     * @return multiplier value for decay rate
     */
    float getDecayRateMultiplier();

    /**
     * Returns set of orders currently on this shelf.
     *
     * @return set of orders currently on this shelf
     */
    Set<Order> getOrders();

    /**
     * Adds an order to this shelf.
     *
     * @param order order to be added to this shelf
     * @return true if order successfully added to shelf, false if the shelf was already full and could not add
     */
    boolean addOrder(Order order);

    /**
     * Removes an order from this shelf.
     *
     * @param order order to be removed from this shelf
     * @return true if order successfully removed this shelf, false if the shelf didn't contain the order
     */
    boolean removeOrder(Order order);

    /**
     * Returns true if shelf contains the order.
     *
     * @param order order to check for containment
     * @return true if order is already on this shelf
     */
    boolean containsOrder(Order order);

    /**
     * Returns a boolean indicating if the shelf is at its limit or not.
     *
     * @return true if shelf is full
     */
    boolean isFull();

    /**
     * Public version of java's Object.clone() method.
     *
     * @return cloned shelf object
     * @throws CloneNotSupportedException
     */
    Object clone() throws CloneNotSupportedException;

}
