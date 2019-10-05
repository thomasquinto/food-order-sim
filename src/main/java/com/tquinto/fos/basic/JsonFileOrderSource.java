package com.tquinto.fos.basic;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.tquinto.fos.Order;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Concrete implementation of the abstract <code>BasicOrderSource</code>.
 *
 * This implementation simulates streaming orders in periodic "batches", where a batch of orders is sent at a specified
 * time interval. The "batch" term refers to the time-grouping- the orders are still sent serially and individually.
 * The size of each batch is dictated by a Poisson Distribution which averages around a certain value, such as 3.25
 * orders per second.
 *
 * This class will parse a JSON formatted file which will consist of an array of orders. Here is a simple example of an
 * array with two orders:
 *
 * [
 *     {
 *      "name": "Banana Split",
 *      "temp": "frozen",
 *      "shelfLife": 20,
 *      "decayRate": 0.63
 *     },
 *     {
 *      "name": "McFlury",
 *      "temp": "frozen",
 *      "shelfLife": 375,
 *      "decayRate": 0.4
 *     }
 *  ]
 */
public class JsonFileOrderSource extends BasicOrderSource {

    private static final String FILE_CHAR_SET = "UTF-8";

    private final File file;

    /**
     * Constructor for a JsonFileOrderSource
     *
     * @param timeUnit unit of time of interval to send batches
     * @param averageOrdersPerTimeUnit average size of order batch sent every <code>timeUnit</code>
     * @param file JSON file from which to parse food orders
     */
    public JsonFileOrderSource(TimeUnit timeUnit, float averageOrdersPerTimeUnit, File file) {
        super(timeUnit, averageOrdersPerTimeUnit);
        this.file = file;
    }

    /**
     * Returns the stream of orders to be consumed by a kitchen.
     *
     * @return stream of orders to be consumed by a kitchen
     */
    @Override
    public Observable<Order> getOrders() {
        JsonReader reader;
        try {
            reader = new JsonReader(new InputStreamReader(new FileInputStream(file), FILE_CHAR_SET));
        } catch (FileNotFoundException | UnsupportedEncodingException ex) {
            return Observable.error(ex);
        }

        // This observable simulates emitting a "batch" of orders per time unit
        Observable<List<Order>> batchPerTimeInterval = Observable.create(emitter -> {
            Gson gson = new GsonBuilder().create();

            try {
                reader.beginArray();

                // used to build up a "batch" of orders intended to be sent at a given time interval
                List<Order> batch = new ArrayList<>();
                int limit = scanToNonEmptyBatch(emitter);

                while (reader.hasNext()) {
                    BasicOrder order = gson.fromJson(reader, BasicOrder.class);
                    order.setTimeUnit(getTimeUnit());

                    if (batch.size() >= limit) {
                        emitter.onNext(batch);
                        batch = new ArrayList<>();
                        limit = scanToNonEmptyBatch(emitter);
                    }

                    batch.add(order);
                }

                // emit remaining orders leftover from last iteration of the reader loop
                if (!batch.isEmpty()) {
                    emitter.onNext(batch);
                }

                reader.close();
            } catch (IOException ex) {
                emitter.onError(ex);
            }

            emitter.onComplete();
        });

        // flatten out the batches to be sent serially after adding a time delay between each batch
        // (concatMap ensures that batches are kept in the original order)
        return batchPerTimeInterval
                .concatMap(batch -> Observable.just(batch)
                        .delay(1, getTimeUnit()))
                .flatMap(Observable::fromIterable);
    }

    /**
     * Since it is possible for some batches (e.g. one batch of orders per time unit) to be of zero length, this will
     * emit an empty batch without having to move the reader iterator forward in the middle of parsing JSON.
     *
     * @param emitter emitter for order batches
     * @return size of next non-empty batch
     */
    private int scanToNonEmptyBatch(ObservableEmitter<List<Order>> emitter) {
        int limit = getRandomNumOrdersPerTimeUnit();
        while (limit == 0) {
            // for each empty batch, emit an empty list
            emitter.onNext(new ArrayList<>());
            limit = getRandomNumOrdersPerTimeUnit();
        }
        return limit;
    }

}
