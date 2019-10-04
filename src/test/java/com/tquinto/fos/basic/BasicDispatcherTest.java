package com.tquinto.fos.basic;

import com.tquinto.fos.Dispatcher;
import com.tquinto.fos.Driver;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for BasicDispatcher.
 */
public class BasicDispatcherTest {

    /**
     * Tests that driver durations are random and within the specified maximum and minimum bounds.
     */
    @Test
    public void testDriveDurationBounds() {
        int low = 0;
        int high = 10;
        int iterations = 20;

        Dispatcher dispatcher = new BasicDispatcher(TimeUnit.SECONDS, low, high);

        boolean isRandom = false;
        int previousDriveDuration = -1;

        for(int i=0; i < iterations; i++) {
            Driver driver = dispatcher.dispatchDriver(new BasicOrder());

            assertTrue(driver.getDriveDuration() >= low);
            assertTrue(driver.getDriveDuration() <= high);

            if (previousDriveDuration != -1 && previousDriveDuration != driver.getDriveDuration()) {
                isRandom = true;
            }
            previousDriveDuration = driver.getDriveDuration();
        }

        // It's statistically highly improbable to get the same value 20 consecutive times
        assertTrue(isRandom);
    }
}
