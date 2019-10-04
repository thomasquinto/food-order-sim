# Food Order Simulator (food-order-sim)

A simple java command-line that simulates a kitchen that receives an order, puts the order on a shelf, and dispatches
a driver to pickup the order.

Kitchens have two types of shelves: temperature-specific shelves, such as "hot", "cold" or "frozen", which can only
hold food orders at their designated temperature type, and the "overflow" shelf that can only be used when a
temperature-specific shelf becomes full. The overflow shelf can hold food orders of any temperature type, but the
overflow shelf may increase the decay rate of orders placed on it. When shelves free up, overflow orders can be
placed back on their temperature-specific shelves.

When an order is placed in the kitchen (or a new order arrives in the order stream) and added to a shelf, a driver
is also dispatched to pickup the order. If the driver arrives before the order fully decays over time (based on a
decay formula), the driver can successfully deliver the food. Orders that fully decay over time are thrown out as
waste. Orders that don't fully decay also can be thrown out as waste if the kitchen's shelves are full and room needs
to be made for incoming orders.

Overflow strategies provide the logic of what the kitchen should do in the cases of:
1) an order arrives and its temperature-designated shelf is full
2) an order arrives and its temperature-designated shelf AND overflow shelf is full
3) an order is removed (either by driver pickup or when an order has fully decayed to waste)

## Installation and Usage

`food-order-sim` uses the [Gradle] (https://gradle.org) build tool to build, run tests and run the command-line application.
You can use the gradle wrapper that comes bundled with the project.

To build and run unit tests (this example uses MacOS command-line syntax):

`./gradlew build`

To run the command-line application (with default arguments):

`./gradlew run`

To run the command-line application with command-line arguments:

`./gradlew run --args="src/main/resources/Engineering_Challenge_-_Orders.json SECONDS 3.25 2 8 15 1 15 1 15 1 20 2 false"`

To clean:

`./gradlew clean`

Food Order Simulator arguments:

1) file path of food order JSON file (string): src/main/resources/Engineering_Challenge_-_Orders.json
2) java time unit (string): SECONDS
3) average number of orders per time unit (float): 3.25
4) minimum driver duration in time units (integer): 2
5) maximum driver duration in time units (integer): 8
6) hot shelf order capacity (integer): 15
7) hot shelf decay rate multiplier (float): 1
8) cold shelf order capacity (integer): 15
9) cold shelf decay rate multiplier (float): 1
10) frozen shelf order capacity (integer): 15
11) frozen shelf decay rate multiplier (float): 1
12) overflow shelf order capacity (integer): 20
13) overflow shelf decay rate multiplier (float): 2
14) verbose mode for display out (boolean): false

All of the output of the simular is written to standard out, and an output file in the project root directory
called `food-order-sim.log`. This log is overwritten by the last program execution.

The textual display output updates whenever an order is added or removed and shows the contents of all shelves. 
It also shows a count of total orders received, picked up, decayed to waste and manually removed as waste.

To get a sense of orders coming in realtime, use the `SECONDS` time unit argument such that orders are sent in
batches (controlled by the `average number of orders` argument) in second intervals. To speed up the simulator
for faster runs and results, you can use `MILLISECONDS`. All other time-based arguments use this same time unit, 
such as minimum and maximum duration for delivery drivers to pickup food orders.

For shelves, the limit capacity can be specified as well as a decay-rate multiplier for orders placed on those
shelves. By default, the hot, cold, and frozen multipliers are 1 and the overflow shelf multiplier is 2.

Lastly, to see more data per order in the output, you can specify `true` for verbose mode.

## Implementation Notes

This section essentially describes how orders are handled when moving to and from the overflow shelf. For more
in-depth implementation details, please see the documentation and source code for 
[com.tquinto.fos.basic.BasicOverflowStrategy] (main/src/com/tquinto/fos/basic/BasicOverflowStrategy.java)

The general strategy employed in this implementation is:

1) When moving orders to the overflow shelf, try to find an order with the longest decay duration (or "time to live"
before it's decay value reaches zero). The hope is that the overflow shelf won't affect "long-living" orders as much
as quickly decaying orders.

2) When moving orders from the overflow shelf, try to find the order with the shortest decay duration to give that
order additional time before expiring, in the hope it will get picked up by a delivery driver in time.

3) When an order of a certain temperature type arrives and its shelf and the overflow shelf are both full, try to
find the order that will expire the soonest and remove that order, since there's already a higher probability that
the order won't be picked up in time to be delivered.

The decay formula is abstracted, with a specific implementation described in the comments for
[com.tquinto.fos.basic.BasicDecayFormula] (main/src/com/tquinto/fos/basic/BasicDecayFormula.java). 

## License

MIT License

## About Me

Thomas Quinto
`thomasq@gmail.com`
[My Linked-In Profile](https://linkedin.com/pub/thomas-quinto/0/b/4a1)

