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

This application was built to satisfy the problem presented in [a relative link] (doc/Software_Engineering_Challenge.pdf)


