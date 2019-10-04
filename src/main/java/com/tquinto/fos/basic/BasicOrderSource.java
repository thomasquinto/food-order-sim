package com.tquinto.fos.basic;

import com.tquinto.fos.Order;
import com.tquinto.fos.OrderSource;
import io.reactivex.Observable;
import org.apache.commons.math3.distribution.PoissonDistribution;

import java.util.concurrent.TimeUnit;

/**
 * Abstraction for a source of orders. This could be anything- a JSON file, a web server where orders are uploaded, etc.
 *
 * This implementation simulates streaming orders in periodic "batches", where a batch of orders is sent at a specified
 * time interval. The "batch" term refers to the time-grouping- the orders are still sent serially and individually.
 * The size of each batch is dictated by a Poisson Distribution which averages around a certain value, such as 3.25
 * orders per second.
 */
public abstract class BasicOrderSource implements OrderSource {

    private final TimeUnit timeUnit;
    private final PoissonDistribution poissonDistribution;

    /**
     * Constructor for a BasicOrderSource
     *
     * @param timeUnit unit of time of interval to send batches
     * @param averageOrdersPerTimeUnit average size of order batch sent every <code>timeUnit</code>
     */
    public BasicOrderSource(TimeUnit timeUnit, float averageOrdersPerTimeUnit) {
        this.timeUnit = timeUnit;
        poissonDistribution = new PoissonDistribution(averageOrdersPerTimeUnit);
    }

    /**
     * Returns the unit of time for each interval at which batches are sent
     *
     * @return unit of time for each interval at which batches are sent
     */
    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    /**
     * Returns a pseudo-random number value based on the average orders sent per time interval
     *
     * @return pseudo-random number value based on the average orders sent per time interval
     */
    public int getRandomNumOrdersPerTimeUnit() {
        return poissonDistribution.sample();
    }

    /**
     * Returns the average number of orders sent per time interval
     *
     * @return average number of orders sent per time interval
     */
    public float getAverageNumOrdersPerTimeUnit() {
        return (float) poissonDistribution.getMean();
    }

    /**
     * Returns the stream of orders to be consumed by a kitchen.
     *
     * @return stream of orders to be consumed by a kitchen
     */
    public abstract Observable<Order> getOrders();

}
