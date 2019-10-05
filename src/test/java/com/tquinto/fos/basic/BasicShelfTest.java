package com.tquinto.fos.basic;

import com.tquinto.fos.Shelf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BasicShelf.
 */
public class BasicShelfTest {

    /**
     * Tests exception being thrown for trying to add order of wrong type/temp.
     */
    @Test
    public void testAddInvalidOrderType() {
        Shelf shelf  = new BasicShelf("hot", 1, 1.0f);
        BasicOrder order1 = new BasicOrder();
        order1.setTemp("cold"); // wrong type

        assertThrows(Shelf.InvalidOrderTypeException.class, () -> {
            shelf.addOrder(order1);
        });
    }

    /**
     * Tests successful addition of order to a shelf.
     */
    @Test
    public void testAddOrder() {
        Shelf shelf  = new BasicShelf("hot", 1, 1.0f);

        assertEquals(1, shelf.getLimit());
        assertFalse(shelf.isFull());

        BasicOrder order1 = new BasicOrder();
        order1.setTemp("hot");

        assertTrue(shelf.addOrder(order1));
        assertTrue(shelf.isFull());
        assertTrue(shelf.containsOrder(order1));

        BasicOrder order2 = new BasicOrder();
        order2.setTemp("hot");

        assertFalse(shelf.addOrder(order2));
        assertFalse(shelf.containsOrder(order2));
    }


    /**
     * Tests successful removal of an order from a shelf.
     */
    @Test
    public void testRemoveOrder() {
        Shelf shelf  = new BasicShelf("hot", 1, 1.0f);
        BasicOrder order1 = new BasicOrder();
        order1.setTemp("hot");

        assertTrue(shelf.addOrder(order1));
        assertTrue(shelf.removeOrder(order1));
        assertFalse(shelf.containsOrder(order1));
        assertFalse(shelf.isFull());
    }

    /**
     * Tests clone of shelf object.
     */
    @Test
    public void testClone() {
        Shelf shelf = new BasicShelf("hot", 2, 1.0f);

        BasicOrder order1 = new BasicOrder();
        BasicOrder order2 = new BasicOrder();

        order1.setTemp("hot");
        order2.setTemp("hot");

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
