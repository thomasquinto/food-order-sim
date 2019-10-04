package com.tquinto.fos.basic;

import com.tquinto.fos.Order;
import com.tquinto.fos.OrderSource;
import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

/**
 * Unit tests for BasicOrderSource.
 */
public class BasicOrderSourceTest {

    private Order getTestOrder() {
        BasicOrder order = new BasicOrder();
        order.setName("Banana Split");
        order.setTemp("frozen");
        order.setShelfLife(20);
        order.setDecayRate(0.63f);

        return order;
    }

    /**
     * Tests a single emission of an order.
     */
    @Test
    public void testOrderEmission() {
        final Order testOrder = getTestOrder();

        OrderSource orderSource = new BasicOrderSource(TimeUnit.SECONDS, 3) {
            @Override
            public Observable<Order> getOrders() {
                return Observable.just(testOrder);
            }
        };

        TestObserver<Order> testObserver = orderSource.getOrders().test();

        testObserver.assertSubscribed();
        testObserver.assertValueCount(1);
        testObserver.assertValue(testOrder);
        testObserver.assertComplete();
        testObserver.dispose();
    }

}
