package com.tquinto.fos.basic;

import com.tquinto.fos.*;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.TestObserver;
import io.reactivex.subjects.PublishSubject;
import org.javatuples.Pair;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BasicKitchen.
 */
public class BasicKitchenTest {

    /**
     * Initialize an order and update decay rate to that decay methods may be invoked.
     */
    private Order initOrder(BasicOrder order, Date date) {
        order.initialize(date);
        order.setTimeUnit(TimeUnit.SECONDS);
        order.updateDecayRate(date, order.getDecayRate());
        return order;
    }

    /**
     * Builds a populated kitchen object for test purposes.
     * @param shelfCapacity size for all shelves in kitchen
     * @param overflowStrategy overflow strategy
     * @return test kitchen instance
     */
    private Kitchen buildKitchen(int shelfCapacity, OverflowStrategy overflowStrategy) {
        final Shelf hotShelf = new BasicShelf("hot", shelfCapacity, 1);
        final Shelf coldShelf = new BasicShelf("cold", shelfCapacity, 1);
        final Shelf frozenShelf = new BasicShelf("frozen", shelfCapacity, 1);
        List<Shelf> shelves = new ArrayList<>(Arrays.asList(hotShelf, coldShelf, frozenShelf));

        final Shelf overflowShelf = new BasicShelf("overflow", shelfCapacity, 2);
        overflowShelf.setAcceptedTypes(new HashSet<>(new ArrayList<>(Arrays.asList("hot", "cold", "frozen"))));

        return new BasicKitchen(
                new BasicOverflowStrategy(),
                new BasicDispatcher(TimeUnit.SECONDS, 10, 10),
                shelves,
                overflowShelf);
    }

    /**
     * Generates order for testing purposes.
     *
     * @return order for testing purposes
     */
    public Order getTestOrder() {
        final BasicOrder order = new BasicOrder();
        order.setName("Banana Split");
        order.setTemp("frozen");
        order.setShelfLife(20);
        order.setDecayRate(0.63f);

        return order;
    }

    /**
     * Tests processing an order that ends up getting added to a shelf.
     */
    @Test
    public void testAddOrderToShelf() {
        final Kitchen kitchen = buildKitchen(1, new BasicOverflowStrategy());
        final Order order = getTestOrder();
        final PublishSubject<Order> subject = PublishSubject.create();
        final OrderSource orderSource = () -> subject.hide();
        final AtomicBoolean isSubscribed = new AtomicBoolean(false);
        final AtomicBoolean isError = new AtomicBoolean(false);
        final AtomicBoolean isComplete = new AtomicBoolean(false);

        kitchen.processOrders(orderSource)
                .subscribe(new Observer<OrderEvent>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        isSubscribed.set(true);
                    }

                    @Override
                    public void onNext(OrderEvent orderEvent) {
                        // verify event corresponds with placed order
                        assertEquals(orderEvent.getOrder(), order);
                        // verify event type
                        assertEquals(orderEvent.getType(), OrderEvent.Type.ADDED_TO_SHELF);
                        // verify order was placed on correct shelf
                        assertTrue(kitchen.getShelf(order.getTemp()).containsOrder(order));
                    }

                    @Override
                    public void onError(Throwable e) {
                        isError.set(true);
                    }

                    @Override
                    public void onComplete() {
                        isComplete.set(true);
                    }
                });

        // verify subscription active
        assertTrue(isSubscribed.get());
        // verify not complete yet
        assertFalse(isComplete.get());
        // verify no error
        assertFalse(isError.get());

        // place order
        subject.onNext(order);

        // verify subscription active
        assertTrue(isSubscribed.get());
        // verify not complete yet
        assertFalse(isComplete.get());
        // verify no error
        assertFalse(isError.get());

        // complete order stream
        subject.onComplete();

        // verify subscription active (driver was not dispatched yet)
        assertTrue(isSubscribed.get());
        // verify not complete yet (driver was not dispatched yet)
        assertFalse(isComplete.get());
        // verify no error
        assertFalse(isError.get());
    }

    /**
     * Tests multiple orders streaming to a kitchen.
     */
    @Test
    public void testAddMultipleOrders() {
        final Kitchen kitchen = buildKitchen(5, new BasicOverflowStrategy());
        final Order order = getTestOrder();
        final PublishSubject<Order> subject = PublishSubject.create();
        OrderSource orderSource = () -> subject.hide();

        TestObserver<OrderEvent> testObserver = new TestObserver<>();
        kitchen.processOrders(orderSource).subscribe(testObserver);

        subject.onNext(order);
        testObserver.assertValueCount(1);

        subject.onNext(initOrder(new BasicOrder("Pressed Juice", "cold", 250, .2f),
                new Date()));
        subject.onNext(initOrder(new BasicOrder("Cheese Pizza", "hot", 200, .76f),
                new Date()));

        testObserver.assertValueCount(3);

        testObserver.dispose();
    }

    /**
     * Builds a kitchen with orders on its shelves for testing.
     */
    private Kitchen buildKitchen(OverflowStrategy overflowStrategy, Date date) {
        Shelf hotShelf = new BasicShelf("hot", 2, 1.0f);
        Shelf coldShelf = new BasicShelf("cold", 2, 1.0f);
        Shelf frozenShelf = new BasicShelf("frozen", 2, 1.0f);
        Shelf overflowShelf = new BasicShelf("overflow", 2, 2.0f);
        overflowShelf.setAcceptedTypes(new HashSet<>(new ArrayList<>(Arrays.asList("hot", "cold", "frozen"))));

        List<Shelf> shelves = new ArrayList<>(Arrays.asList(hotShelf, coldShelf, frozenShelf));

        hotShelf.addOrder(
                initOrder(new BasicOrder("Cheese Pizza", "hot", 200, .76f), date));
        hotShelf.addOrder(
                initOrder(new BasicOrder("Onion Rings", "hot", 201, .7f), date));

        coldShelf.addOrder(
                initOrder(new BasicOrder("Pressed Juice", "cold", 250, .2f), date));
        coldShelf.addOrder(
                initOrder(new BasicOrder("Kombucha", "cold", 246, .19f), date));

        frozenShelf.addOrder(
                initOrder(new BasicOrder("Popsicle", "frozen", 345, .75f), date));
        frozenShelf.addOrder(
                initOrder(new BasicOrder("McFlury", "frozen", 372, .45f), date));

        overflowShelf.addOrder(
                initOrder(new BasicOrder("Apples", "cold", 244, .23f * 2), date));
        overflowShelf.addOrder(
                initOrder(new BasicOrder("Poppers", "hot", 204, .78f * 2), date));

        Kitchen kitchen = new BasicKitchen(overflowStrategy,
                new BasicDispatcher(TimeUnit.SECONDS, 0, 10),
                shelves,
                overflowShelf);

        return kitchen;
    }

    /**
     * Test that the OverflowStrategy.InvalidProcedureException exception gets thrown in an error scenario
     */
    @Test
    public void testStrategyInvalidProcedureException() {

        // overflow strategy intentionally returns bogus orders
        OverflowStrategy overflowStrategy = new OverflowStrategy() {
            @Override
            public Order onTempShelfFull(Kitchen kitchen, Order incomingOrder, Date date) {
                return null;
            }

            @Override
            public Pair<Order, Order> onOverflowShelfFull(Kitchen kitchen, Order incomingOrder, Date date) {
                Set<Order> orders = kitchen.getShelf("cold").getOrders();
                Order removalCandidate = null;
                for (Order order : orders) {
                    removalCandidate = order;
                    break;
                }
                return Pair.with(removalCandidate, null);
            }

            @Override
            public Order onOrderRemoved(Kitchen kitchen, Order removedOrder, Date date) {
                return null;
            }
        };

        final Kitchen kitchen = buildKitchen(overflowStrategy, new Date());
        final Order order = getTestOrder();
        final PublishSubject<Order> subject = PublishSubject.create();
        OrderSource orderSource = () -> subject.hide();

        TestObserver<OrderEvent> testObserver = new TestObserver<>();
        kitchen.processOrders(orderSource).subscribe(testObserver);

        subject.onNext(order);

        testObserver.assertError(OverflowStrategy.InvalidProcedureException.class);

        testObserver.dispose();
    }

}
