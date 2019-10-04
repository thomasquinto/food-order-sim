package com.tquinto.fos.basic;

import com.tquinto.fos.Kitchen;
import com.tquinto.fos.Order;
import com.tquinto.fos.OverflowStrategy;
import com.tquinto.fos.Shelf;
import org.javatuples.Pair;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class BasicOverflowStrategyTest {

    private Order initOrder(BasicOrder order, Date date) {
        order.initialize(date);
        order.setTimeUnit(TimeUnit.SECONDS);
        order.updateDecayRate(date, order.getDecayRate());
        return order;
    }

    private Kitchen buildKitchen(OverflowStrategy overflowStrategy, Date date) {
        Shelf hotShelf = new BasicShelf("hot", 2, 1.0f);
        Shelf coldShelf = new BasicShelf("cold", 2, 1.0f);
        Shelf frozenShelf = new BasicShelf("frozen", 2, 1.0f);
        Shelf overflowShelf = new BasicShelf("overflow", 2, 2.0f);

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

    @Test
    public void testRemoveIncomingOrder() {
        OverflowStrategy overflowStrategy = new BasicOverflowStrategy();
        Date date = new Date();
        Kitchen kitchen = buildKitchen(overflowStrategy, date);

        Order incomingOrder = initOrder(
                new BasicOrder("Banana Split", "frozen", 20, .43f), date);
        Pair<Order,Order> orderPair = overflowStrategy.onOverflowShelfFull(kitchen, incomingOrder, date);

        assertEquals(incomingOrder, orderPair.getValue0());
        assertNull(orderPair.getValue1());
    }

    @Test
    public void testRemoveOverflowOrder() {

    }

    @Test
    public void testRemoveTempOrder() {

    }
}
