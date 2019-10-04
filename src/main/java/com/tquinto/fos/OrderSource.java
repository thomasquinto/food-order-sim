package com.tquinto.fos;

import io.reactivex.Observable;

/**
 * Abstraction for a source of orders. This could be anything- a JSON file, a web server where orders are uploaded, etc.
 */
public interface OrderSource {

    /**
     * Returns a stream of orders as they are placed in realtime.
     *
     * @return a stream of orders
     */
    Observable<Order> getOrders();

}
