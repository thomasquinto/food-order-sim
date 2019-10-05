package com.tquinto.fos.basic;

import com.tquinto.fos.*;
import io.reactivex.*;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import org.javatuples.Pair;

import java.util.*;

/**
 * Simple implementation for a kitchen that receives orders, puts the orders on a shelf, and dispatches a drivers to
 * pickup the orders.
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
public class BasicKitchen implements Kitchen {

    private OverflowStrategy overflowStrategy;
    private Dispatcher dispatcher;
    private Map<String, Shelf> shelves;
    private Shelf overflowShelf;
    private Subject<OrderEvent> orderEventSubject;
    private boolean allOrdersProcessed;

    private final Map<Order, Disposable> decayDisposables;
    private final Map<Order, Disposable> driverDisposables;

    /**
     * Constructor for a BasicKitchen.
     *
     * @param overflowStrategy strategy to determine how to shift items when all shelves are full or which order from
     *                         the overflow shelf to replace a picked up or wasted order
     * @param dispatcher Dispatcher of drivers to pickup orders
     * @param temperatureShelves shelves that only accept orders of a certain temperature
     * @param overflowShelf overflow shelf that can only hold items when a shelf of a designated temperature is full
     */
    public BasicKitchen(OverflowStrategy overflowStrategy,
                        Dispatcher dispatcher,
                        List<Shelf> temperatureShelves,
                        Shelf overflowShelf) {

        this.overflowStrategy = overflowStrategy;
        this.dispatcher = dispatcher;

        shelves = new HashMap<>();
        for (Shelf shelf : temperatureShelves) {
            shelves.put(shelf.getType(), shelf);
        }

        this.overflowShelf = overflowShelf;

        decayDisposables = Collections.synchronizedMap(new HashMap<>());
        driverDisposables = Collections.synchronizedMap(new HashMap<>());
    }

    /**
     * Returns a set of all temperature-specific shelf types (i.e. "hot", "cold", or "frozen").
     * This does not include the "overflow" shelf.
     *
     * @return set of all shelf temperature types
     */
    @Override
    public Set<String> getShelfTemps() {
        return shelves.keySet();
    }

    /**
     * Returns a shelf for a given temperature, such as "hot", "cold", or "frozen".
     * This does not include the "overflow" shelf.
     *
     * @param temp temperature type of shelf
     * @return shelf of the requested temperature type
     */
    @Override
    public Shelf getShelf(String temp) {
        return shelves.get(temp);
    }

    /**
     * Returns the overflow shelf, where orders can be placed when a certain temperature-specific shelf is full.
     * The overflow shelf can hold orders of any temperature ("hot", "cold", or "frozen").
     *
     * @return overflow shelf
     */
    @Override
    public Shelf getOverflowShelf() {
        return overflowShelf;
    }

    /**
     * A kitchen processes orders from an order source, which will provide a stream of orders.
     *
     * @param orderSource source of orders
     * @return observable that emits a stream of resulting order events that chronicles when orders are added to
     * shelves, picked up by drivers, decay to waste, or are manually removed as waste
     */
    @Override
    public Observable<OrderEvent> processOrders(OrderSource orderSource) {

        orderSource.getOrders()
                .subscribe(new Observer<Order>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        orderEventSubject = PublishSubject.create();
                    }

                    @Override
                    public void onNext(Order order) {
                        processOrder(order);
                    }

                    @Override
                    public void onError(Throwable e) {
                        orderEventSubject.onError(e);
                    }

                    @Override
                    public void onComplete() {
                        allOrdersProcessed = true;
                        checkForCompletion();
                    }
                });

        return orderEventSubject.hide();
    }

    // Private methods section below

    /**
     * Processes a single incoming order.
     * @param order order that was just placed from the order source
     */
    private synchronized void processOrder(Order order) {
        Date date = new Date();
        order.initialize(date);

        dispatchDriver(order);

        if (getShelf(order.getTemp()).addOrder(order)) {
            // successfully added to shelf of appropriate temperature (hot, cold, or frozen)
            orderAddedToShelf(date, order, getShelf(order.getTemp()));
        } else if (!getOverflowShelf().isFull()) {
            // designated temperature shelf is full, so ask the overflow strategy which order from the shelf (or the
            // incoming order) should be placed on the overflow shelf where there is still room
            shiftOrdersOnTempShelfFull(date, order, overflowStrategy.onTempShelfFull(this, order, date));
        } else {
            // designated temperature shelf is full AND overflow shelf is full
            Pair<Order, Order> ordersResult = overflowStrategy.onOverflowShelfFull(this, order, date);
            shiftOrdersOnOverflowShelfFull(date, order, ordersResult.getValue0(), ordersResult.getValue1());
        }
    }

    /**
     * Called immediately after an order is placed on a shelf.
     * @param date date at which the order was placed on a shelf
     * @param order order that was added to a shelf
     * @param shelf shelf the order was added to
     */
    private void orderAddedToShelf(Date date, Order order, Shelf shelf) {
        cancelPreviousSubscription(decayDisposables, order);

        // update decay rate for order since it's being added to a new shelf
        order.updateDecayRate(date, order.getDecayRate() * shelf.getDecayRateMultiplier());

        // round time up to nearest time unit, which will guarantee order will be fully decayed by then
        final int decayDuration = (int) Math.ceil((double) order.getDecayDuration(date));

        decayDisposables.put(order,
                Single.create((SingleOnSubscribe<Order>) emitter -> emitter.onSuccess(order))
                        .delay(decayDuration, order.getTimeUnit())
                        .subscribe(this::orderDecayed)
        );

        sendOrderEvent(order, OrderEvent.Type.ADDED_TO_SHELF, shelf.getType(), date);
    }

    /**
     * Moves an order of a given temperature to the overflow shelf since the designated temperature shelf is full
     * (and there is room on the overflow shelf).
     *
     * @param date current date of move
     * @param incomingOrder incoming order that just arrived and hasn't been added to a shelf yet
     * @param orderToMoveToOverflow order (either incoming or on a shelf) that needs to be moved to overflow shelf
     */
    private void shiftOrdersOnTempShelfFull(Date date, Order incomingOrder, Order orderToMoveToOverflow) {
        if (incomingOrder == orderToMoveToOverflow) {
            if (getOverflowShelf().addOrder(incomingOrder)) {
                orderAddedToShelf(date, orderToMoveToOverflow, getOverflowShelf());
            } else {
                throw new OverflowStrategy.InvalidProcedureException(
                        "failed to add incoming order to overflow shelf");
            }
        } else {
            // if order is on a designated temperature shelf (e.g. not the incoming order), remove it first
            if (getShelf(orderToMoveToOverflow.getTemp()).removeOrder(orderToMoveToOverflow)) {
                if (getOverflowShelf().addOrder(orderToMoveToOverflow)) {
                    // successfully added to overflow shelf
                    orderAddedToShelf(date, orderToMoveToOverflow, getOverflowShelf());

                    // now add incoming order as replacement for order added to overflow shelf
                    if (getShelf(orderToMoveToOverflow.getTemp()).addOrder(incomingOrder)) {
                        orderAddedToShelf(date, incomingOrder, getShelf(orderToMoveToOverflow.getTemp()));
                    } else {
                        throw new OverflowStrategy.InvalidProcedureException(
                                "failed to move incoming order replacement to native shelf");
                    }
                } else {
                    throw new OverflowStrategy.InvalidProcedureException(
                            "failed to add overflow candidate to overflow shelf");
                }
            } else {
                throw new OverflowStrategy.InvalidProcedureException(
                        "failed to remove overflow candidate from native shelf");
            }
        }
    }

    /**
     * Shifts orders around when all shelves are full. The order to be removed and the order to replace the removed
     * order are both chosen by the overflow strategy.
     *
     * There are exactly 3 scenarios:
     *
     * 1) Incoming order is immediately removed and thrown out as waste and no other orders are shifted
     *
     * 2) An order is removed from its designated temperature shelf, and is either replaced by the incoming order or
     * an order from the overflow shelf (in which case the incoming order replaces the overflow order)
     *
     * 3) An order is removed from the overflow shelf, and is either replaced by the incoming order, or is replaced
     * by an order already on a shelf (in which case the incoming order replaces the spot on the temperature shelf)
     *
     * @param incomingOrder incoming order that just arrived and hasn't been added to a shelf yet
     * @param wasteOrder order to be removed and thrown out as waste
     * @param replacementOrder order to replace removed order (this parameter is ignored if the incoming order is
     *                         selected as waste order)
     */
    private void shiftOrdersOnOverflowShelfFull(Date date, Order incomingOrder, Order wasteOrder,
                                                Order replacementOrder) {

        if (getShelf(wasteOrder.getTemp()).removeOrder(wasteOrder)) {
            // order was removed from a "temperature" shelf
            Shelf removedFromShelf = getShelf(wasteOrder.getTemp());
            orderWasted(date, wasteOrder, removedFromShelf);

            if(replacementOrder == incomingOrder) {
                // incoming order takes the removed order's place
                if(removedFromShelf.addOrder(incomingOrder)) {
                    orderAddedToShelf(date, incomingOrder, removedFromShelf);
                } else {
                    throw new OverflowStrategy.InvalidProcedureException(
                            "failed to remove incoming error from native shelf");
                }
            } else {
                // replacement must be from overflow shelf, so remove it from the overflow shelf and move it to the
                // shelf with the open spot, and then move incoming order to the overflow shelf
                if (getOverflowShelf().removeOrder(replacementOrder)) {
                    if (removedFromShelf.addOrder(replacementOrder)) {
                        orderAddedToShelf(date, replacementOrder, removedFromShelf);
                    } else {
                        throw new OverflowStrategy.InvalidProcedureException(
                                "failed to add replacement candidate to new shelf");
                    }
                    if (getOverflowShelf().addOrder(incomingOrder)) {
                        orderAddedToShelf(date, incomingOrder, getOverflowShelf());
                    } else {
                        throw new OverflowStrategy.InvalidProcedureException(
                                "failed to add incoming order from overflow shelf");
                    }
                } else {
                    throw new OverflowStrategy.InvalidProcedureException(
                            "failed to remove replacement candidate from overflow shelf");
                }
            }
        } else if (getOverflowShelf().removeOrder(wasteOrder)) {
            // order was removed from overflow shelf
            orderWasted(date, wasteOrder, getOverflowShelf());

            if(replacementOrder == incomingOrder) {
                // incoming order takes removed overflow order's place
                if(getOverflowShelf().addOrder(incomingOrder)) {
                    orderAddedToShelf(date, incomingOrder, getOverflowShelf());
                } else {
                    throw new OverflowStrategy.InvalidProcedureException(
                            "failed to add incoming order to overflow shelf");
                }
            } else {
                // remove replacement order from its shelf and move to overflow shelf, and then move incoming order
                // into its place
                Shelf tempShelf = getShelf(replacementOrder.getTemp());
                if (tempShelf.removeOrder(replacementOrder)) {
                    if (getOverflowShelf().addOrder(replacementOrder)) {
                        orderAddedToShelf(date, replacementOrder, getOverflowShelf());
                    } else {
                        throw new OverflowStrategy.InvalidProcedureException(
                                "failed to add replacement candidate to overflow shelf");
                    }
                    if (tempShelf.addOrder(incomingOrder)) {
                        orderAddedToShelf(date, incomingOrder, tempShelf);
                    } else {
                        throw new OverflowStrategy.InvalidProcedureException(
                                "failed to add incoming order to native shelf");
                    }
                } else {
                    throw new OverflowStrategy.InvalidProcedureException(
                            "failed to remove replacement candidate from native shelf");
                }
            }
        } else {
            // must be incoming order since it wasn't on any shelf, so immediately throw out as waste
            orderWasted(date, wasteOrder, null);
        }
    }

    /**
     * Callback invoked when an order has reached a decay value of zero, meaning the order must be removed and thrown
     * out as waste.
     *
     * @param order order that has reached a decay value of zero
     */
    private synchronized void orderDecayed(Order order) {
        Date date = new Date();

        if (getShelf(order.getTemp()).removeOrder(order)) {
            orderWasted(date, order, getShelf(order.getTemp()));
            // replace open spot with an order from overflow shelf
            replaceWithOverflowItem(order, getShelf(order.getTemp()), date);
        } else if (getOverflowShelf().removeOrder(order)) {
            orderWasted(date, order, getOverflowShelf());
        }

        checkForCompletion();
    }

    /**
     * Called immediately after an order has been thrown out as waste because it's decay value has reached zero.
     *
     * @param date date at which order was removed as waste
     * @param order order that has fully decayed to waste
     * @param shelf shelf the order was removed from
     */
    private void orderWasted(Date date, Order order, Shelf shelf) {
        cancelSubscriptions(order);
        String shelfType = shelf != null ? shelf.getType() : null;
        sendOrderEvent(order, OrderEvent.Type.REMOVED_WASTE, shelfType, date);
    }

    /**
     * Called immediately after a driver has picked up an order.
     *
     * @param date date at which order was picked up
     * @param order order that was picked up by driver
     * @param shelf shelf from which the order was picked up from
     */
    private void orderPickedUp(Date date, Order order, Shelf shelf) {
        cancelSubscriptions(order);
        String shelfType = shelf != null ? shelf.getType() : null;
        sendOrderEvent(order, OrderEvent.Type.PICKED_UP, shelfType, date);
    }


    /**
     * Dispatches a driver to pickup a designated order.
     *
     * @param order order for drive to pickup
     */
    private void dispatchDriver(Order order) {
        final Driver driver = dispatcher.dispatchDriver(order);

        driverDisposables.put(order,
                Single.create((SingleOnSubscribe<Driver>) emitter -> emitter.onSuccess(driver))
                        .delay(driver.getDriveDuration(), driver.getTimeUnit())
                        .subscribe(this::driverArrivedForOrderPickup)
        );
    }

    /**
     * Callback invoked when a driver arrives to pickup an order.
     *
     * @param driver driver who is picking up the order
     */
    private synchronized void driverArrivedForOrderPickup(Driver driver) {
        Date date = new Date();
        Order order = driver.getOrder();

        // Find whether order is on its designated temperature shelf, or is on the overflow shelf
        Shelf shelf = getShelf(order.getTemp());
        if (!shelf.containsOrder(order)) {
            shelf = getOverflowShelf();
            if (!getOverflowShelf().containsOrder(order)) {
                shelf = null;
            }
        }

        if (shelf != null) {
            boolean orderRemoved = false;
            if (order.getDecayValue(date) <= 0 && shelf.removeOrder(order)) {
                // order decayed before driver could pick up, so throw out as waste
                // NOTE: this can only happen since order decay delay duration is rounded up to nearest integer time
                // unit, meaning a race condition could arise if the driver picks up within the rounding delta
                orderRemoved = true;
                orderWasted(date, order, shelf);
            } else if (shelf.removeOrder(order)) {
                // order was removed from its current shelf
                orderRemoved = true;
                // mark as picked up
                orderPickedUp(date, order, shelf);
            }

            if (orderRemoved && shelf != getOverflowShelf()) {
                // a spot has freed up for an overflow item to replace
                replaceWithOverflowItem(order, shelf, date);
            }
        }

        checkForCompletion();
    }

    /**
     * Replace recently open spot on shelf with an available overflow order.
     *
     * @param removedOrder order just removed
     * @param shelf shelf with open spot
     * @param date
     */
    private void replaceWithOverflowItem(Order removedOrder, Shelf shelf, Date date) {
        if (shelf != getOverflowShelf()) {
            // Replace the open spot with the order selected by the overflow strategy
            Order overflowOrder = overflowStrategy.onOrderRemoved(this, removedOrder, date);
            if (overflowOrder != null) {
                if (getOverflowShelf().removeOrder(overflowOrder)) {

                    if (shelf.addOrder(overflowOrder)) {
                        orderAddedToShelf(date, overflowOrder, shelf);
                    } else {
                        throw new OverflowStrategy.InvalidProcedureException(
                                "failed to add overflow order to new shelf");
                    }
                } else {
                    throw new OverflowStrategy.InvalidProcedureException(
                            "failed to remove order from overflow shelf");
                }
            }
        }
    }

    /**
     * Cancels any active subscriptions associated with an order.
     *
     * @param order order that was removed or pickup
     */
    private void cancelSubscriptions(Order order) {
        cancelPreviousSubscription(decayDisposables, order);
        cancelPreviousSubscription(driverDisposables, order);
    }

    /**
     * For a given order, find if there was a previous subscription and cancel it
     *
     * @param disposableMap map with order as the key and associated disposable as the value
     * @param order order to cancel subscriptions for
     */
    private void cancelPreviousSubscription(Map<Order,Disposable> disposableMap, Order order) {
        Disposable previousSubscription = disposableMap.get(order);
        if (previousSubscription != null) {
            previousSubscription.dispose();
            disposableMap.remove(order);
        }
    }

    /**
     * Returns true if there are no more orders that are scheduled to decay or to be picked up, and the stream of
     * incoming orders has also completed
     *
     * @return true if there are no more orders that are scheduled to decay or to be picked up, and the stream of
     * incoming orders has also completed
     */
    private boolean checkForCompletion() {
        if (allOrdersProcessed && decayDisposables.isEmpty() && driverDisposables.isEmpty()) {
            orderEventSubject.onComplete();
            return true;
        }

        return false;
    }

    /**
     * Using the order event subject, sends an order event that contains the state of the kitchen at the time the event
     * occurred.
     *
     * @param order order associated with event
     * @param type order event type (i.e. ADDED_TO_SHELF, PICKED_UP, etc.)
     * @param shelfType type of shelf the order was added to or removed from (can be null in the case an incoming order
     *                  is immediately thrown out as waste and never added to a shelf)
     * @param date date at which event occurred
     */
    private void sendOrderEvent(Order order, OrderEvent.Type type, String shelfType, Date date) {
        // Build a list of the shelves to show exact shelf contents at the time of this event
        final List<Shelf> shelfList = new ArrayList<>();
        for (Shelf shelf : shelves.values()) {
            try {
                // clone shelf to make a "frozen-in-time" copy
                shelfList.add((Shelf) shelf.clone());
            } catch(CloneNotSupportedException ex) {
                orderEventSubject.onError(ex);
                return;
            }
        }

        try {
            // clone shelf to make a "frozen-in-time" copy
            shelfList.add((Shelf) overflowShelf.clone());
        } catch(CloneNotSupportedException ex) {
            orderEventSubject.onError(ex);
            return;
        }

        orderEventSubject.onNext(new BasicOrderEvent(order, type, shelfType, date, shelfList));
    }

}
