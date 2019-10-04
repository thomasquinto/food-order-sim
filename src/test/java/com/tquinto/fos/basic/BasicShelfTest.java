package com.tquinto.fos.basic;

import com.tquinto.fos.Order;
import com.tquinto.fos.Shelf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BasicShelfTest {

    @Test
    public void testAddOrder() {
        Shelf shelf  = new BasicShelf("hot", 1, 1.0f);

        assertEquals(1, shelf.getLimit());
        assertFalse(shelf.isFull());

        Order order1 = new BasicOrder();

        assertTrue(shelf.addOrder(order1));
        assertTrue(shelf.isFull());
        assertTrue(shelf.containsOrder(order1));

        Order order2 = new BasicOrder();

        assertFalse(shelf.addOrder(order2));
        assertFalse(shelf.containsOrder(order2));
    }

    @Test
    public void testRemoveOrder() {
        Shelf shelf  = new BasicShelf("hot", 1, 1.0f);
        Order order1 = new BasicOrder();

        assertTrue(shelf.addOrder(order1));
        assertTrue(shelf.removeOrder(order1));
        assertFalse(shelf.containsOrder(order1));
        assertFalse(shelf.isFull());
    }

    @Test
    public void testClone() {
        Shelf shelf = new BasicShelf("hot", 2, 1.0f);

        Order order1 = new BasicOrder();
        Order order2 = new BasicOrder();

        shelf.addOrder(order1);
        shelf.addOrder(order2);

        Shelf clonedShelf = null;
        try {
            clonedShelf = (Shelf) shelf.clone();
        } catch(CloneNotSupportedException ex) {
            ex.printStackTrace();
            return;
        }

        assertNotEquals(shelf, clonedShelf);
        assertEquals(shelf.getType(), clonedShelf.getType());
        assertEquals(shelf.getLimit(), clonedShelf.getLimit());
        assertEquals(shelf.getDecayRateMultiplier(), clonedShelf.getDecayRateMultiplier());
        assertTrue(shelf.containsOrder(order1));
        assertTrue(shelf.containsOrder(order2));
        assertTrue(clonedShelf.containsOrder(order1));
        assertTrue(clonedShelf.containsOrder(order2));
    }
}
