package com.tquinto.fos.basic;

import com.tquinto.fos.*;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.TestObserver;
import io.reactivex.subjects.PublishSubject;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BasicKitchen.
 */
public class BasicKitchenTest {

    /**
     * Builds a populated kitchen object for test purposes.
     * @param shelfCapacity size for all shelves in kitchen
     * @param dispatchDriverImmediately set to true for no wait for driver to pickup order
     * @return test kitchen instance
     */
    private Kitchen buildKitchen(int shelfCapacity, boolean dispatchDriverImmediately) {
        final Shelf hotShelf = new BasicShelf("hot", shelfCapacity, 1);
        final Shelf coldShelf = new BasicShelf("cold", shelfCapacity, 1);
        final Shelf frozenShelf = new BasicShelf("frozen", shelfCapacity, 1);
        List<Shelf> shelves = new ArrayList<>(Arrays.asList(hotShelf, coldShelf, frozenShelf));

        final Shelf overflowShelf = new BasicShelf("overflow", shelfCapacity, 2);

        int driverDuration = dispatchDriverImmediately ? 0 : Integer.MAX_VALUE;

        return new BasicKitchen(
                new BasicOverflowStrategy(),
                new BasicDispatcher(TimeUnit.SECONDS, driverDuration, driverDuration),
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
        final Kitchen kitchen = buildKitchen(1, false);
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

    /*
    @Test
    public void testOrderPickedUp() {
        final Kitchen kitchen = buildKitchen(1, true);
        final Order order = getTestOrder();
        final PublishSubject<Order> subject = PublishSubject.create();
        final OrderSource orderSource = () -> subject.hide();
        final AtomicInteger orderEventCount = new AtomicInteger(0);

        Disposable disposable = kitchen.processOrders(orderSource)
                .subscribe(orderEvent -> {
                        System.out.println("onNext: " +
                                new BasicOrderEventDisplay(null, false)
                                        .formatOrderEvent(orderEvent));
                        // verify event corresponds with placed order
                        assertEquals(orderEvent.getOrder(), order);

                        if (orderEventCount.get() == 0) {
                            // first event is order added to shelf
                            orderEventCount.set(orderEventCount.get() + 1);
                            // verify event corresponds with placed order
                            assertEquals(orderEvent.getOrder(), order);
                            // verify event type
                            assertEquals(orderEvent.getType(), OrderEvent.Type.ADDED_TO_SHELF);
                            // verify order was placed on correct shelf
                            assertTrue(kitchen.getShelf(order.getTemp()).containsOrder(order));
                        } else if (orderEventCount.get() == 1) {
                            // second event is order picked up by driver
                            orderEventCount.set(orderEventCount.get() + 1);
                            // verify event corresponds with placed order
                            assertEquals(orderEvent.getOrder(), order);
                            // verify event type
                            assertEquals(orderEvent.getType(), OrderEvent.Type.PICKED_UP);
                            // verify order was removed from shelf
                            assertFalse(kitchen.getShelf(order.getTemp()).containsOrder(order));
                        }
                    });

        // place order
        subject.onNext(order);

        assertEquals(2, orderEventCount.get());

        // complete order stream
        subject.onComplete();

        disposable.dispose();
    }
    */

    @Test
    public void testAddOrderToShelf_BROKEN1() {
        final Kitchen kitchen = buildKitchen(5, false);
        final Order order = getTestOrder();
        final PublishSubject<Order> subject = PublishSubject.create();
        OrderSource orderSource = () -> subject.hide();

        TestObserver<OrderEvent> testObserver = new TestObserver<>();
        kitchen.processOrders(orderSource).subscribe(testObserver);

        System.out.println("GOT HERE 1");

        subject.onNext(order);
        testObserver.assertValueCount(1);

        System.out.println("GOT HERE 2");

        testObserver.awaitCount(2);

        // BROKEN:
        subject.onComplete();
        testObserver.assertComplete();
    }

    @Test
    public void testAddOrderToShelf_BROKEN2() {
        Kitchen kitchen = buildKitchen(5, false);

        final Order order = getTestOrder();

        OrderSource orderSource = () -> Observable.just((Order) order);

        TestObserver<OrderEvent> testObserver = kitchen.processOrders(orderSource).test();

        testObserver.awaitCount(1);

        testObserver.assertValueCount(1);
        testObserver.dispose();
    }

}
