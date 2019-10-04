package com.tquinto.fos.basic;

import com.tquinto.fos.Order;
import com.tquinto.fos.Shelf;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * An abstraction for a shelf in a kitchen which holds orders to be picked up by a driver.
 */
public class BasicShelf implements Shelf {

    private String type;
    private Set<String> acceptedTypes = new HashSet<>();
    private int limit;
    private float decayRateMultiplier;

    private Set<Order> orders = Collections.synchronizedSet(new HashSet<>());

    /**
     * Constructor for Shelf object
     *
     * @param limit maximum amount of orders that can be added to shelf
     * @param type type of shelf (i.e. "hot", "cold", "frozen", "overflow")
     * @param decayRateMultiplier multiplier for decay rate for orders added to this shelf
     */
    public BasicShelf(String type, int limit, float decayRateMultiplier) {
        this.type = type;
        this.limit = limit;
        this.decayRateMultiplier = decayRateMultiplier;

        acceptedTypes.add(type);
    }

    /**
     * Type of shelf.
     *
     * For "temperature" shelves, orders have the same "temp" value as the shelf's type.
     * @return String value of shelf type (i.e. "hot", "cold", "frozen", "overflow")
     */
    @Override
    public String getType() {
        return type;
    }

    /**
     * Returns string array of the types of orders this shelf accepts.
     *
     * @return String array of accepted order types.
     */
    public Set<String> getAcceptedTypes() {
        return acceptedTypes;
    }

    /**
     * Sets the accepted order types for this shelf. If an order is added for any other type, then an
     * <code>InvalidOrderTypeException</code> will be thrown.
     * @param acceptedTypes
     */
    public void setAcceptedTypes(Set<String> acceptedTypes) {
        this.acceptedTypes = acceptedTypes;
    }

    /**
     * Returns limit of number orders that can be added to this shelf.
     *
     * @return limit of number orders that can be added to this shelf
     */
    @Override
    public int getLimit() {
        return limit;
    }

    /**
     * Multiplier applied to decay rates of orders added to this shelf.
     *
     * @return multiplier value for decay rate
     */
    @Override
    public float getDecayRateMultiplier() {
        return decayRateMultiplier;
    }

    /**
     * Returns set of orders currently on this shelf.
     *
     * @return set of orders currently on this shelf
     */
    @Override
    public Set<Order> getOrders() {
        return orders;
    }

    /**
     * Adds an order to this shelf.
     *
     * @param order order to be added to this shelf
     * @return true if order successfully added to shelf, false if the shelf was already full and could not add
     */
    @Override
    public boolean addOrder(Order order) {
        if (!acceptedTypes.contains(order.getTemp())) {
            throw new InvalidOrderTypeException(String.format("Invalid order type: %s", order.getTemp()));
        }

        if (orders.size() < limit) {
            return orders.add(order);
        }
        return false;
    }

    /**
     * Removes an order from this shelf
     *
     * @param order order to be removed from this shelf
     * @return true if order successfully removed this shelf, false if the shelf didn't contain the order
     */
    @Override
    public boolean removeOrder(Order order) {
        return orders.remove(order);
    }

    /**
     * Returns true if shelf contains the order.
     *
     * @param order order to check for containment
     * @return true if order is already on this shelf
     */
    @Override
    public boolean containsOrder(Order order) {
        return orders.contains(order);
    }

    /**
     * Returns a boolean indicating if the shelf is at its limit or not.
     *
     * @return true if shelf is full
     */
    @Override
    public boolean isFull() {
        return orders.size() >= limit;
    }

    /**
     * Public version of java's Object.clone() method.
     *
     * @return cloned shelf object
     * @throws CloneNotSupportedException
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        BasicShelf shelf = (BasicShelf) super.clone();

        // perform a deep copy and clone every order (since each order maintains some state at a given time)
        shelf.orders = Collections.synchronizedSet(new HashSet<>());
        for (Order order : orders) {
            shelf.orders.add((Order) order.clone());
        }

        return shelf;
    }

}
