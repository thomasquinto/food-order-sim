package com.tquinto.fos.basic;

import com.tquinto.fos.Kitchen;
import com.tquinto.fos.Order;
import com.tquinto.fos.OverflowStrategy;
import com.tquinto.fos.Shelf;
import org.javatuples.Pair;

import java.util.*;

/**
 * Simple implementation for an overflow strategy for a kitchen which dictates the logic of moving orders to and from
 * the overflow shelf.
 *
 * The general strategy employed in this implementation is:
 *
 * 1) When moving orders to the overflow shelf, try to find an order with the longest decay duration (or "time to live"
 * before it's decay value reaches zero). The hope is that the overflow shelf won't affect "long-living" orders as much
 * as quickly decaying orders.
 *
 * 2) When moving orders from the overflow shelf, try to find the order with the shortest decay duration to give that
 * order additional time before expiring, in the hope it will get picked up by a delivery driver in time.
 *
 * 3) When an order of a certain temperature type arrives and its shelf and the overflow shelf are both full, try to
 * find the order that will expire the soonest and remove that order, since there's already a higher probability that
 * the order won't be picked up in time to be delivered.
 */
public class BasicOverflowStrategy implements OverflowStrategy {

    /**
     * Invoked by a kitchen object when an order is received and its designated temperature shelf is full, such that
     * either the incoming order or an order on the shelf of the designated temperature (of the incoming order) needs
     * to be moved to the overflow shelf.
     *
     * This implementation takes the incoming order and all of the orders on the corresponding temperature shelf and
     * recalculates the decay duration for each order as if they were on the overflow shelf (which multiplies the decay
     * rate by some factor). The order with the longest adjusted decay duration (or "time to live" on the overflow
     * shelf) becomes the order that will be moved to the overflow shelf.
     *
     * @param kitchen kitchen object
     * @param incomingOrder incoming order just received by the kitchen before being placed on a shelf
     * @param date date at which the incoming order is received by the kitchen
     * @return order that will be put on the overflow shelf
     */
    public Order onTempShelfFull(Kitchen kitchen, Order incomingOrder, Date date) {
        Shelf tempShelf = kitchen.getShelf(incomingOrder.getTemp());
        List<Order> orders = new ArrayList<>(tempShelf.getOrders());
        orders.add(incomingOrder);

        // temporarily adjust decay rate according to overflow shelf
        adjustDecayRates(kitchen, orders, date, kitchen.getOverflowShelf().getDecayRateMultiplier());

        Order orderToMoveToOverflow = selectOrderWithLongestDecayDuration(orders, date);

        // reset decay rates back to original values
        adjustDecayRates(kitchen, orders, date, null);

        return orderToMoveToOverflow;
    }

    /**
     * Invoked by a kitchen object when an order of a certain temperature ("hot", "cold", or "frozen") is full and
     * the overflow shelf is also full, such that an order needs to be removed.
     *
     * The general strategy is to remove the order that will expire (fully decay to waste) the soonest. We can use
     * decay duration (the amount of time remaining before full decay) as the comparison mechanism between orders.
     *
     * This strategy employs this workflow:
     *
     * 1) Find the lowest decay duration from the following orders:
     *
     *  - incoming order
     *  - all the orders on the overflow shelf
     *  - all orders on (non-overflow) full shelves where either the incoming order or at least one overflow order is
     *  of the same temperature (such that an order could be replaced from elsewhere if an order is removed from the
     *  shelf)
     *
     *  This order will become the "removal candidate".
     *
     * 2) If the removal candidate is the incoming order, remove it out and leave the rest of the orders alone and
     * return that result.
     *
     * 3) If the removal candidate is on a normal (non-overflow) shelf, find a replacement order from the overflow
     * shelf. The strategy is to find the order from the overflow shelf of the same temperature as the removal
     * candidate that will expire the soonest (e.g. has the lowest decay duration) to give that order more "time to
     * live" when it's taken off the overflow shelf. If the incoming order is also of the same temperature as the
     * removal candidate, also consider it as a replacement based on the same criteria (lowest decay duration)
     * compared against the overflow orders.
     *
     * 4) If the removal candidate is on the overflow shelf, build a list of all of the orders from all full shelves-
     * including the incoming order. For all of the orders in that list, adjust the decay duration as if the orders
     * were on the overflow shelf (which multiplies the decay rate by some factor). The order that will "live the
     * longest" on the overflow shelf becomes the one selected to replace the overflow removal candidate.
     *
     * @param kitchen Kitchen object
     * @param incomingOrder incoming order (order was just placed and not yet on a shelf)
     * @param date date when incoming order is received
     * @return Pair object with two orders: 1) order to remove, 2) order to replace vacated spot of removed item
     */
    @Override
    public Pair<Order, Order> onOverflowShelfFull(Kitchen kitchen, Order incomingOrder, Date date) {
        // build a list of potential removal candidates
        List<Order> candidates = new ArrayList<>();

        // add incoming order
        candidates.add(incomingOrder);

        // add all orders from the overflow shelf
        candidates.addAll(kitchen.getOverflowShelf().getOrders());

        // add all orders belonging to full shelves eligible for replacement:
        List<Order> eligibleTempOrders = getEligibleTempOrders(kitchen, incomingOrder);
        candidates.addAll(eligibleTempOrders);

        Order removalCandidate = selectOrderWithShortestDecayDuration(candidates, date);

        if (removalCandidate == incomingOrder) {

            // removal candidate is the incoming order, no other orders need be moved around
            return Pair.with(incomingOrder, null);

        } else if (kitchen.getShelf(removalCandidate.getTemp()).containsOrder(removalCandidate)) {

            // removal candidate is on a normal (non-overflow) shelf, so find a replacement
            List<Order> replacementCandidates = getOrdersForTemp(kitchen.getOverflowShelf().getOrders(),
                    removalCandidate.getTemp());

            if (incomingOrder.getTemp().equals(removalCandidate.getTemp())) {
                // also add incoming order since its of the same temperature to be a replacement
                replacementCandidates.add(incomingOrder);
            }

            Order replacementCandidate = selectOrderWithShortestDecayDuration(replacementCandidates, date);
            return Pair.with(removalCandidate, replacementCandidate);

        } else {
            // removal candidate must be on overflow shelf

            // build a list of replacement candidates, which in turn would need replaced by the incoming order when
            // it moves to the overflow shelf to replace the removed overflow order, so it must be the same
            // temperature type as the incoming order
            List<Order> replacementCandidates = new ArrayList<>();
            for (Order order : eligibleTempOrders) {
                if (order.getTemp().equals(incomingOrder.getTemp())) {
                    replacementCandidates.add(order);
                }
            }

            // also add incoming order as a potential replacement candidate
            replacementCandidates.add(incomingOrder);

            // now adjust decay rates for all replacement candidates as if they were on overflow shelf
            adjustDecayRates(kitchen, replacementCandidates, date, kitchen.getOverflowShelf().getDecayRateMultiplier());

            // pick the candidate with the longest to live on overflow shelf
            Order replacementCandidate = selectOrderWithLongestDecayDuration(replacementCandidates, date);

            // reset decay rates back to original values
            adjustDecayRates(kitchen, replacementCandidates, date, null);

            return Pair.with(removalCandidate, replacementCandidate);
        }
    }

    /**
     * Invoked by a kitchen object when an order either expired (fully decayed) or was picked up by a driver.
     * This gives the strategy the opportunity to move an order from the overflow shelf to replace the removed order.
     *
     * This implementation selects the order from the overflow shelf with the shortest decay duration, or the order
     * that is about to expire the soonest (to give that order more "time to live" when it's taken off the overflow
     * shelf).
     *
     * @param kitchen kitchen containing shelves and orders
     * @param removedOrder order that was just removed from a shelf
     * @param date date when order was removed from shelf
     */
    @Override
    public Order onOrderRemoved(Kitchen kitchen, Order removedOrder, Date date) {
        // eligible candidates to replace the removed order are overflow orders of the appropriate temperature
        List<Order> candidates = getOrdersForTemp(kitchen.getOverflowShelf().getOrders(), removedOrder.getTemp());

        // select overflow order that will fully decay the soonest
        if (!candidates.isEmpty()) {
            return selectOrderWithShortestDecayDuration(candidates, date);
        }

        return null;
    }

    /**
     * Adjusts the decay rate for each order in a list depending on a shelf's decay rate multiplier.
     *
     * @param kitchen kitchen object
     * @param orders list of orders
     * @param date current date
     * @param decayRateMultiplier decay rate multiplier to multiply order decay rate- if null, then use the decay rate
     *                            multiplier of the order's designated temperature shelf
     */
    private void adjustDecayRates(Kitchen kitchen, List<Order> orders, Date date, Float decayRateMultiplier) {
        for (Order order : orders) {
            decayRateMultiplier = decayRateMultiplier != null ?
                    decayRateMultiplier : kitchen.getShelf(order.getTemp()).getDecayRateMultiplier();
            order.updateDecayRate(date, decayRateMultiplier * order.getDecayRate());
        }
    }

    /**
     * Returns a list of orders comprised of orders from all non-overflow shelves that are full and could be replaced
     *
     * @param kitchen kitchen object
     * @param Order incoming order
     * @return list of orders comprised of orders from all non-overflow shelves that are full
     */
    private List<Order> getEligibleTempOrders(Kitchen kitchen, Order incomingOrder) {
        Set<String> overflowTemps = new HashSet<>();
        for (Order order: kitchen.getOverflowShelf().getOrders()) {
            overflowTemps.add(order.getTemp());
        }
        overflowTemps.add(incomingOrder.getTemp());

        List<Order> orders = new ArrayList<>();
        for (String temp : overflowTemps) {
            if (kitchen.getShelf(temp).isFull()) {
                orders.addAll(kitchen.getShelf(temp).getOrders());
            }
        }
        return orders;
    }

    /**
     * Given a set of orders, returns a filtered list of orders that are of a certain temperature type.
     *
     * @param orders set of orders to filter by temperature type
     * @param temp temperature type to filter orders
     * @return a filtered list of orders that are of a certain temperature type
     */
    private List<Order> getOrdersForTemp(Set<Order> orders, String temp) {
        List<Order> tempOrders = new ArrayList<>();
        for (Order order : orders) {
            if (temp.equals(order.getTemp())) {
                tempOrders.add(order);
            }
        }
        return tempOrders;
    }

    /**
     * Finds order with shortest decay duration.
     *
     * @param candidates list of candidates to search from
     * @param date current date from which to calculate decay duration
     * @return order with shortest decay duration
     */
    private Order selectOrderWithShortestDecayDuration(List<Order> candidates, Date date) {
        candidates.sort((o1, o2) -> (int) (o1.getDecayDuration(date) - o2.getDecayDuration(date)));
        return candidates.get(0);
    }

    /**
     * Finds order with longest decay duration.
     *
     * @param candidates list of candidates to search from
     * @param date current date from which to calculate decay duration
     * @return order with longest decay duration
     */
    private Order selectOrderWithLongestDecayDuration(List<Order> candidates, Date date) {
        candidates.sort((o1, o2) -> (int) (o2.getDecayDuration(date) - o1.getDecayDuration(date)));
        return candidates.get(0);
    }

}
