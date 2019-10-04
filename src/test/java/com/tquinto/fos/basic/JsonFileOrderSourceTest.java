package com.tquinto.fos.basic;

import com.google.gson.JsonParseException;
import com.tquinto.fos.Order;
import io.reactivex.observers.TestObserver;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * Unit tests for JsonFileOrderSource.
 */
public class JsonFileOrderSourceTest {

    private static final String TEST_JSON_FILE =
            "src/test/resources/Engineering_Challenge_-_Orders.json";
    private static final String TEST_JSON_FILE_CORRUPT_FILE =
            "src/test/resources/Engineering_Challenge_-_Orders_corrupt_file.json";
    private static final String TEST_JSON_FILE_CORRUPT_ITEM =
            "src/test/resources/Engineering_Challenge_-_Orders_corrupt_item.json";

    /**
     * Tests parsing of an actual JSON file by checking number of title JSON objects read.
     */
    @Test
    public void testParsingJsonFile() {
        // This file contains a total of 132 orders
        JsonFileOrderSource orderSource = new JsonFileOrderSource(TimeUnit.MILLISECONDS, 3.25f,
                new File(TEST_JSON_FILE));

        TestObserver<Order> testObserver = new TestObserver<>();
        orderSource.getOrders().blockingSubscribe(testObserver);

        testObserver.assertValueCount(132);
        testObserver.dispose();
    }

    /**
     * Tests parsing of an incorrectly formatted JSON file.
     */
    @Test
    public void testParsingCorruptJsonFile() {
        // This file is breaks JSON format intentionally at the first item
        JsonFileOrderSource orderSource = new JsonFileOrderSource(TimeUnit.MILLISECONDS, 3.25f,
                new File(TEST_JSON_FILE_CORRUPT_FILE));

        TestObserver<Order> testObserver = new TestObserver<>();
        orderSource.getOrders().blockingSubscribe(testObserver);

        testObserver.assertValueCount(0);
        testObserver.assertError(JsonParseException.class);
        testObserver.dispose();
    }

    /**
     * Tests parsing of a JSON file with one item that has a field of a wrong type (i.e. string instead of number)
     */
    @Test
    public void testParsingJsonFileWithCorruptItem() {
        // This file has an order with an incorrectly typed field (string instead of number)
        JsonFileOrderSource orderSource = new JsonFileOrderSource(TimeUnit.MILLISECONDS, 3.25f,
                new File(TEST_JSON_FILE_CORRUPT_ITEM));

        TestObserver<Order> testObserver = new TestObserver<>();
        orderSource.getOrders().blockingSubscribe(testObserver);

        testObserver.assertError(JsonParseException.class);
        testObserver.dispose();
    }
}
