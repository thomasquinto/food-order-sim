package com.tquinto.fos;

import io.reactivex.Observable;

/**
 * Abstraction for a display that updates whenever an order is added or removed and shows the contents of all shelves.
 */
public interface OrderEventDisplay {

    /**
     * Displays events consumed for an order event stream.
     * @param orderEventObservable order event stream (usually provided by a kitchen)
     */
    void display(Observable<OrderEvent> orderEventObservable);

}
