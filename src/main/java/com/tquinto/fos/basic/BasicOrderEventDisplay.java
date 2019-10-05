package com.tquinto.fos.basic;

import com.tquinto.fos.Order;
import com.tquinto.fos.OrderEvent;
import com.tquinto.fos.OrderEventDisplay;
import com.tquinto.fos.Shelf;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Implementation for a display that updates whenever an order is added or removed and shows the contents of all
 * shelves. This implementation shows a simple text-based display of order events and contents of the shelves.
 * It also shows a count of total orders received, picked up, and wasted.
 */
public class BasicOrderEventDisplay implements OrderEventDisplay {

    /**
     * Interface for custom formatters to format the output of order events.
     */
    interface Formatter {
        /**
         * Formats an order event into textual output.
         *
         * @param orderEvent order event to be formatted
         * @param isVerboseMode show extra (verbose) fields for order event
         * @return String textual output of order event.
         */
        String formatOrderEvent(OrderEvent orderEvent, boolean isVerboseMode);
    }

    private Formatter formatter;
    private String outputFileName;
    private boolean outputToStdOut;
    private PrintWriter printWriter;
    private boolean isVerboseMode;
    private Set<Long> orderIds = new HashSet<>();

    private int decayedCount;
    private int removedCount;
    private int pickupCount;

    /**
     * Constructor for BasicOrderEventDisplay
     *
     * @param outputFileName file path for display output (can be null)
     * @param outputToStdOut set to true for output to sent to the standard out console
     * @param isVerboseMode set to true to see additional order event properties
     */
    public BasicOrderEventDisplay(Formatter formatter, String outputFileName, boolean outputToStdOut, boolean isVerboseMode) {
        this.formatter = formatter;
        this.outputFileName = outputFileName;
        this.outputToStdOut = outputToStdOut;
        this.isVerboseMode = isVerboseMode;
    }

    /**
     * Constructor for BasicOrderEventDisplay
     *
     * @param outputFileName file path for display output (can be null)
     * @param outputToStdOut set to true for output to sent to the standard out console
     * @param isVerboseMode set to true to see additional order event properties
     */
    public BasicOrderEventDisplay(String outputFileName, boolean outputToStdOut, boolean isVerboseMode) {
        this.formatter = new BasicFormatter();
        this.outputFileName = outputFileName;
        this.outputToStdOut = outputToStdOut;
        this.isVerboseMode = isVerboseMode;
    }

    /**
     * Displays events consumed for an order event stream.
     *
     * @param orderEventObservable order event stream (usually provided by a kitchen)
     */
    public void display(Observable<OrderEvent> orderEventObservable) {
        orderEventObservable.blockingSubscribe(new Observer<OrderEvent>() {
            @Override
            public void onSubscribe(Disposable d) {
                if (outputFileName != null) {
                    try {
                        FileWriter fileWriter = new FileWriter(outputFileName);
                        printWriter = new PrintWriter(fileWriter);
                    } catch (IOException ioException) {
                        onError(ioException);
                    }
                }
            }

            @Override
            public void onNext(OrderEvent orderEvent) {
                orderIds.add(orderEvent.getOrder().getId());

                switch(orderEvent.getType()) {
                    case PICKED_UP:
                        pickupCount++;
                        break;
                    case REMOVED_WASTE:
                        removedCount++;
                        break;
                    case DECAYED_WASTE:
                        decayedCount++;
                        break;
                }

                if (printWriter != null) {
                    printWriter.println(formatter.formatOrderEvent(orderEvent, isVerboseMode));
                }
                if (outputToStdOut) {
                    System.out.println(formatter.formatOrderEvent(orderEvent, isVerboseMode));
                }
            }

            @Override
            public void onError(Throwable e) {
                if (printWriter != null) {
                    e.printStackTrace(printWriter);
                }
                if (outputToStdOut) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onComplete() {
                if (printWriter != null) {
                    printWriter.close();
                }
            }
        });
    }

    /**
     * Simple implementation of order event formatter that formats each event for output or display.
     */
    public class BasicFormatter implements Formatter {

        /**
         * Returns formatted string of an order event
         *
         * @param orderEvent order event to display
         * @return formatted string of an order event
         */
        public String formatOrderEvent(OrderEvent orderEvent, boolean isVerboseMode) {
            return String.format("\n\n%s%s\n%s\n%s%s\n%s",
                    orderEvent.getType(),
                    formatShelfType(orderEvent.getShelfType()),
                    formatOrder(orderEvent.getOrder(), orderEvent.getDate(), isVerboseMode),
                    orderEvent.getDate(),
                    formatShelves(orderEvent.getShelves(), orderEvent.getDate(), isVerboseMode),
                    formatCounts());
        }

        /**
         * Returns formatted string of a shelf type
         *
         * @param shelfType shelf life to display
         * @return formatted string of a shelf type
         */
        private String formatShelfType(String shelfType) {
            return String.format(" - %s shelf", shelfType);
        }

        /**
         * Returns formatted string of an order
         *
         * @param order order to display
         * @param date  date of order event
         * @return formatted string of an order
         */
        private String formatOrder(Order order, Date date, boolean isVerboseMode) {

            if (isVerboseMode) {
                return String.format("Order{" +
                                "id=%d" +
                                ", name='%s'" +
                                ", temp='%s'" +
                                ", shelfLife=%d" +
                                ", decayRate=%.2f" +
                                ", normalizedDecayValue=%.2f" +
                                ", decayValue=%.2f" +
                                ", decayDuration=%.2f" +
                                ", birthDate=%s" +
                                ", addedToShelfDate=%s" +
                                ", currentDecayRate=%.2f" +
                                ", adjustedShelfLife=%d" +
                                "}",
                        order.getId(),
                        order.getName(),
                        order.getTemp(),
                        order.getShelfLife(),
                        order.getDecayRate(),
                        order.getNormalizedDecayValue(date),
                        order.getDecayValue(date),
                        order.getDecayDuration(date),
                        order.getBirthDate(),
                        order.getAddedToShelfDate(),
                        order.getCurrentDecayRate(),
                        order.getAdjustedShelfLife()
                );
            }

            return String.format("Order{" +
                            "id=%d" +
                            ", name='%s'" +
                            ", temp='%s'" +
                            ", shelfLife=%d" +
                            ", decayRate=%.2f" +
                            ", normalizedDecayValue=%.4f" +
                            "}",
                    order.getId(),
                    order.getName(),
                    order.getTemp(),
                    order.getShelfLife(),
                    order.getDecayRate(),
                    order.getNormalizedDecayValue(date)
            );
        }

        /**
         * Returns formatted string of a list of shelves
         *
         * @param shelves list of shelves to display
         * @param date    date of order event
         * @return formatted string of a list of shelves
         */
        private String formatShelves(List<Shelf> shelves, Date date, boolean isVerboseMode) {
            StringBuffer buffer = new StringBuffer();
            for (Shelf shelf : shelves) {
                buffer.append(String.format("\n%s", formatShelf(shelf, date, isVerboseMode)));
            }
            return buffer.toString();
        }

        /**
         * Returns formatted string of a shelf
         *
         * @param shelf shelf to display
         * @param date  date of order event
         * @return formatted string of a shelf
         */
        private String formatShelf(Shelf shelf, Date date, boolean isVerboseMode) throws NumberFormatException {
            StringBuffer buffer = new StringBuffer();
            buffer.append(String.format("%s shelf size: %d ", shelf.getType(), shelf.getOrders().size()));

            Set<Order> orderedSet = new TreeSet<>((o1, o2) -> (int) (o1.getId() - o2.getId()));
            orderedSet.addAll(shelf.getOrders());

            for (Order order : orderedSet) {
                buffer.append("\n");
                buffer.append(formatOrder(order, date, isVerboseMode));
            }

            return buffer.toString();
        }

        /**
         * Returns formatted string of summarized order counts
         *
         * @return formatted string of summarized order counts
         */
        private String formatCounts() {
            return String.format("orders received: %d, picked up: %d, decayed: %d, removed: %d",
                    orderIds.size(), pickupCount, decayedCount, removedCount);
        }
    }
}
