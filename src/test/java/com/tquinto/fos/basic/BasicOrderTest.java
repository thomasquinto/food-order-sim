package com.tquinto.fos.basic;

import com.tquinto.fos.Order;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BasicOrder.
 */
public class BasicOrderTest {

    /**
     * Tests BasicOrder object equality.
     */
    @Test
    public void testObjectEquality() {
        BasicOrder order1 = new BasicOrder();
        BasicOrder order2 = order1;

        // same order, just different assignments
        assertEquals(order1, order2);

        BasicOrder order3 = new BasicOrder();
        // different orders
        assertNotEquals(order1, order3);
    }

    /**
     * Tests adding and removing orders from a Set.
     */
    @Test
    public void testSetUniqueness() {
        BasicOrder order1 = new BasicOrder();

        // create a set and add order
        Set<Order> orderSet = new HashSet<>();
        orderSet.add(order1);
        assertEquals(orderSet.size(), 1);
        assertTrue(orderSet.contains(order1));

        // test addition fails because set already contains order
        assertFalse(orderSet.add(order1));

        // test addition of a new distinct order
        BasicOrder order2 = new BasicOrder();
        orderSet.add(order2);
        assertEquals(orderSet.size(), 2);
        assertTrue(orderSet.contains(order2));

        // remove order and verify set contents
        assertTrue(orderSet.remove(order2));
        assertEquals(orderSet.size(), 1);
        assertFalse(orderSet.contains(order2));
        assertTrue(orderSet.contains(order1));
    }

    /**
     * Tests cloning orders.
     */
    @Test
    public void testClone() {
        BasicOrder order1 = new BasicOrder();
        Date date1 = new Date();
        order1.initialize(date1);

        BasicOrder order2;
        try {
            order2 = (BasicOrder) order1.clone();
        } catch (CloneNotSupportedException ex) {
            ex.printStackTrace();
            return;
        }

        assertEquals(order1.getId(), order2.getId());
        assertEquals(order1.getBirthDate(), order2.getBirthDate());

        Date date2 = new Date(date1.getTime() + TimeUnit.SECONDS.toMillis(60));
        order2.initialize(date2);

        assertNotEquals(order1.getBirthDate(), order2.getBirthDate());
    }

    /**
     * Test not-initialized exception.
     */
    @Test
    public void testNotInitializedException() {
        BasicOrder order = new BasicOrder();
        order.setShelfLife(300);
        order.setDecayRate(.45f);
        order.setTimeUnit(TimeUnit.SECONDS);

        // initialized intentionally not called

        assertThrows(Order.NotInitializedException.class, () -> {
            order.getDecayDuration(new Date());
        });
    }

    /**
     * Tests decay value, normalized decay value and decay duration of an order over time.
     */
    @Test
    public void testDecay() {

        // This particular example will decay to zero after 206.9 seconds (with a constant decay rate)
        BasicOrder order = new BasicOrder();
        order.setShelfLife(300);
        order.setDecayRate(.45f);
        order.setTimeUnit(TimeUnit.SECONDS);

        Date date1 = new Date();
        order.initialize(date1);

        assertEquals(300, order.getShelfLife());
        assertEquals(.45f, order.getDecayRate());
        assertEquals(TimeUnit.SECONDS, order.getTimeUnit());
        assertEquals(date1, order.getBirthDate());
        assertEquals(1.0f, order.getNormalizedDecayValue(date1));
        assertEquals(300, order.getDecayValue(date1));

        float decayDuration1 = order.getDecayDuration(date1);

        Date date2 = new Date(date1.getTime() + TimeUnit.MILLISECONDS.convert(60, order.getTimeUnit()));
        order.updateDecayRate(date2, order.getDecayRate());
        float decayDuration2 = order.getDecayDuration(date2);

        assertEquals(((int) decayDuration1 - (int) decayDuration2), 60);
        assertEquals(order.getNormalizedDecayValue(date2), order.getDecayValue(date2) / order.getShelfLife());

        Date date3 = new Date(date1.getTime() + TimeUnit.MILLISECONDS.convert(120, order.getTimeUnit()));
        order.updateDecayRate(date3, order.getDecayRate());
        float decayDuration3 = order.getDecayDuration(date3);

        assertEquals(((int) decayDuration1 - (int) decayDuration3), 120);
        assertEquals(order.getNormalizedDecayValue(date3), order.getDecayValue(date3) / order.getShelfLife());

        Date date4 = new Date(date1.getTime() + TimeUnit.MILLISECONDS.convert(180, order.getTimeUnit()));
        order.updateDecayRate(date4, order.getDecayRate());
        float decayDuration4 = order.getDecayDuration(date4);

        assertEquals(((int) decayDuration1 - (int) decayDuration4), 180);
        assertEquals(order.getNormalizedDecayValue(date4), order.getDecayValue(date4) / order.getShelfLife());

        Date date5 = new Date(date1.getTime() + TimeUnit.MILLISECONDS.convert(206, order.getTimeUnit()));
        order.updateDecayRate(date5, order.getDecayRate());
        float decayDuration5 = order.getDecayDuration(date5);

        assertEquals(((int) decayDuration1 - (int) decayDuration5), 206);
        assertEquals(order.getNormalizedDecayValue(date5), order.getDecayValue(date5) / order.getShelfLife());
        assertTrue(order.getDecayDuration(date5) < 1.0f);
        assertTrue(order.getDecayValue(date5) > 0);

        Date date6 = new Date(date1.getTime() + TimeUnit.MILLISECONDS.convert(207, order.getTimeUnit()));
        order.updateDecayRate(date6, order.getDecayRate());
        assertTrue(order.getDecayValue(date6) <= 0);
    }

    /**
     * Test temporarily changing decay rate and back again.
     */
    @Test
    public void testTemporaryDecayChanges() {
        BasicOrder order = new BasicOrder();
        order.setShelfLife(300);
        order.setDecayRate(.45f);
        order.setTimeUnit(TimeUnit.SECONDS);

        Date date = new Date();
        order.initialize(date);

        assertEquals(order.getDecayDuration(date), 206.89655f);

        order.updateDecayRate(date, order.getDecayRate() * 2);
        assertEquals(order.getCurrentDecayRate(), order.getDecayRate() * 2);

        order.updateDecayRate(date, order.getDecayRate());
        assertEquals(order.getCurrentDecayRate(), order.getDecayRate());
        assertEquals(order.getDecayDuration(date), 206.89655f);
    }
}
