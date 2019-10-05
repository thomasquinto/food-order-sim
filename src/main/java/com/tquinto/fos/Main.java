package com.tquinto.fos;

import com.tquinto.fos.basic.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Main entry-point to running the Food Order Simulator via command-line.
 */
public class Main {

    /**
     * Default file that shows output from the food order simulation.
     */
    static final String OUTPUT_FILE = "food-order-sim.log";

    static final String[] ARGUMENT_DESCRIPTORS = new String[] {
            "file path of food order JSON file (string)",
            "java time unit (string)",
            "average number of orders per time unit (float)",
            "minimum driver duration in time units (integer)",
            "maximum driver duration in time units (integer)",
            "hot shelf order capacity (integer)",
            "hot shelf decay rate multiplier (float)",
            "cold shelf order capacity (integer)",
            "cold shelf decay rate multiplier (float)",
            "frozen shelf order capacity (integer)",
            "frozen shelf decay rate multiplier (float)",
            "overflow shelf order capacity (integer)",
            "overflow shelf decay rate multiplier (float)",
            "verbose mode for output (boolean)"
    };

    /**
     * Default arguments for the food order simulator when no arguments are supplied.
     */
    static final String[] ARGUMENT_DEFAULTS = new String[] {
            "src/main/resources/Engineering_Challenge_-_Orders.json",
            TimeUnit.SECONDS.name(),
            "3.25",
            "2",
            "8",
            "15",
            "1",
            "15",
            "1",
            "15",
            "1",
            "20",
            "2",
            "false"
    };

    /**
     * Main entry point.
     * @param args arguments for food order simulation
     */
    public static void main(String[] args) {
        if (args == null || args.length == 0) {
            args = ARGUMENT_DEFAULTS;
        } else if (!verifyArguments(args)) {
            return;
        }

        runFoodOrderSimulation(args);
    }

    /**
     * Verifies command-line arguments.
     * @param args string array of user-entered command-line arguments
     * @return false if wrong number of arguments supplied
     */
    private static boolean verifyArguments(String[] args) {
        if (args == null || args.length != ARGUMENT_DEFAULTS.length) {
            System.out.println(
                    "Wrong number of arguments.\n\n" +
                            "Argument list:");
            for (int i = 0; i < ARGUMENT_DESCRIPTORS.length; i++) {
                System.out.println(String.format("%d) %s, default: %s", i + 1, ARGUMENT_DESCRIPTORS[i], ARGUMENT_DEFAULTS[i]));
            }

            System.out.println("Example gradle command to run food order simulator (to copy and paste):\n");
            System.out.print("./gradlew run --args=\"");
            for (int i = 0; i < ARGUMENT_DEFAULTS.length; i++) {
                if (i < ARGUMENT_DEFAULTS.length - 1) {
                    System.out.print(String.format("%s ", ARGUMENT_DEFAULTS[i]));
                } else {
                    System.out.print(String.format("%s", ARGUMENT_DEFAULTS[i]));
                }
            }
            System.out.print("\"");

            System.out.println("\n\nTo run using defaults, omit all arguments.");
            return false;
        }

        return true;
    }

    /**
     * Shows arguments before running food order simulator.
     * @param args arguments for food order simulator
     */
    private static void showArguments(String[] args) {
        System.out.println("Food Order Simulator arguments:\n");
        for (int i = 0; i < ARGUMENT_DESCRIPTORS.length; i++) {
            System.out.println(String.format("%d) %s: %s", i + 1, ARGUMENT_DESCRIPTORS[i], args[i]));
        }
        System.out.println("\nPress Ctrl-C to exit, simulator will begin in a second...");

        // Add a short delay to show arguments before running simulator
        try {
            Thread.sleep(TimeUnit.SECONDS.toMillis(4));
        } catch(InterruptedException ie) {
            ie.printStackTrace();
        }
    }

    /**
     * Runs the food order simulator.
     * @param args arguments to configure food order simulator
     */
    private static void runFoodOrderSimulation(String[] args) {
        showArguments(args);

        int argIndex = 0;

        // Extract Order Source arguments
        File file = new File(args[argIndex++]);
        TimeUnit timeUnit = TimeUnit.valueOf(args[argIndex++]);
        float averageOrdersPerTimeUnit = Float.parseFloat(args[argIndex++]);

        JsonFileOrderSource orderSource = new JsonFileOrderSource(timeUnit, averageOrdersPerTimeUnit, file);

        // Extract Dispatcher arguments

        int minDriveDuration = Integer.parseInt(args[argIndex++]);
        int maxDriveDuration = Integer.parseInt(args[argIndex++]);

        Dispatcher dispatcher = new BasicDispatcher(timeUnit, minDriveDuration, maxDriveDuration);

        // Extract Shelf arguments

        int hotShelfCapacity = Integer.parseInt(args[argIndex++]);
        float hotShelfDecayRateMultiplier = Float.parseFloat(args[argIndex++]);
        int coldShelfCapacity = Integer.parseInt(args[argIndex++]);
        float coldShelfDecayRateMultiplier = Float.parseFloat(args[argIndex++]);
        int frozenShelfCapacity = Integer.parseInt(args[argIndex++]);
        float frozenShelfDecayRateMultiplier = Float.parseFloat(args[argIndex++]);
        int overflowShelfCapacity = Integer.parseInt(args[argIndex++]);
        float overflowShelfDecayRateMultiplier = Float.parseFloat(args[argIndex++]);

        Shelf hotShelf = new BasicShelf("hot", hotShelfCapacity, hotShelfDecayRateMultiplier);
        Shelf coldShelf = new BasicShelf("cold", coldShelfCapacity, coldShelfDecayRateMultiplier);
        Shelf frozenShelf = new BasicShelf("frozen", frozenShelfCapacity, frozenShelfDecayRateMultiplier);
        Shelf overflowShelf = new BasicShelf("overflow", overflowShelfCapacity, overflowShelfDecayRateMultiplier);
        overflowShelf.setAcceptedTypes(new HashSet<>(new ArrayList<>(Arrays.asList("hot", "cold", "frozen"))));

        List<Shelf> temperatureShelves = new ArrayList<>();
        temperatureShelves.add(hotShelf);
        temperatureShelves.add(coldShelf);
        temperatureShelves.add(frozenShelf);

        // Create kitchen object

        Kitchen kitchen = new BasicKitchen(
                new BasicOverflowStrategy(),
                dispatcher,
                temperatureShelves,
                overflowShelf
        );

        // Begin simulator by processing orders and show results

        boolean isVerboseMode = Boolean.parseBoolean(args[argIndex]);

        new BasicOrderEventDisplay(OUTPUT_FILE, true, isVerboseMode)
                .display(kitchen.processOrders(orderSource));
    }
}
