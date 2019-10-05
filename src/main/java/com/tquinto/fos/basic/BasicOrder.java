package com.tquinto.fos.basic;

import com.tquinto.fos.DecayFormula;
import com.tquinto.fos.Order;

import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * An abstraction for a food order that is placed with a kitchen, is added to a shelf in the kitchen, and then picked up
 * by a driver dispatched to deliver the order. The order decays over time after being added to a shelf which is
 * calculated by a decay formula. Fully decayed orders are thrown out as waste.
 *
 * This implementation assigns a unique id to each order similar to a monotonically increasing database sequence.
 */
public class BasicOrder implements Order {

    private static long sOrderIdGenerator = 0;

    private long id;
    private DecayFormula decayFormula;
    private String name;
    private String temp;
    private int shelfLife;
    private float decayRate;
    private Date birthDate = new Date();
    private TimeUnit timeUnit = TimeUnit.SECONDS;
    public float currentDecayRate;
    public Date addedToShelfDate;
    public int adjustedShelfLife;

    /**
     * Default constructor for a BasicOrder. Uses <code>BasicDecayFormula</code> for the decay formula implementation.
     */
    public BasicOrder() {
        this(new BasicDecayFormula());
    }

    /**
     * Constructor for a BasicOrder.
     *
     * @param decayFormula decay formula implementation that determines decay value and decay duration
     */
    public BasicOrder(DecayFormula decayFormula) {
        this.id = sOrderIdGenerator++;
        this.decayFormula = decayFormula;
    }

    /**
     * Constructor for a BasicOrder.
     *
     * @param name name of order
     * @param temp temperature category of the order, i.e. "hot", "cold", or "frozen"
     * @param shelfLife shelf life of order
     * @param decayRate decay rate of order
     */
    public BasicOrder(String name, String temp, int shelfLife, float decayRate) {
        this();
        this.name = name;
        this.temp = temp;
        this.shelfLife = shelfLife;
        this.decayRate = decayRate;
    }

    /**
     * A unique identifier for the order. No two distinct orders will have the same id.
     *
     * @return unique id of order
     */
    @Override
    public long getId() {
        return id;
    }

    /**
     * Name field of the order.
     *
     * @return name of order
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Setter for name.
     * @param name name to set for order
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Temperature category of the order, i.e. "hot", "cold", or "frozen"
     *
     * @return temperature value of order
     */
    @Override
    public String getTemp() {
        return temp;
    }

    /**
     * Setter for temperature field
     * @param temp temperature field to set for order
     */
    public void setTemp(String temp) {
        this.temp = temp;
    }

    /**
     * Shelf life of order. Used by the food order decay formula to determine when an order becomes waste after some
     * duration of time.
     *
     * @return shelf life of order
     */
    @Override
    public int getShelfLife() {
        return shelfLife;
    }

    /**
     * Setter for shelf life of this order
     * @param shelfLife shelf life value to set for this order
     */
    public void setShelfLife(int shelfLife) {
        this.shelfLife = shelfLife;
    }

    /**
     * Decay rate of order. Used by the food order decay formula to calculate when an order becomes waste after some
     * duration of time.
     *
     * @return decay rate of order used by decay formula
     */
    @Override
    public float getDecayRate() {
        return decayRate;
    }

    /**
     * Sets decay rate of order used by decay formula
     *
     * @param decayRate decay rate of order used by decay formula
     */
    public void setDecayRate(float decayRate) {
        this.decayRate = decayRate;
    }

    /**
     * Used to initialize the internal state of an order when the order is placed. This must be called before retrieving
     * the decay value, normalized decay value and decay duration. Invoked by kitchen when the order is first placed.
     *
     * @param birthDate date the order was added to a shelf
     */
    @Override
    public void initialize(Date birthDate) {
        this.birthDate = birthDate;
        this.addedToShelfDate = birthDate;
        this.adjustedShelfLife = getShelfLife();
        this.currentDecayRate = getDecayRate();
    }

    /**
     * Birth date of order, or when the order is originally placed with the kitchen.
     *
     * @return date the order was placed
     */
    @Override
    public Date getBirthDate() {
        return birthDate;
    }

    /**
     * Time unit of order, such as SECONDS. Applies to shelf life and decay duration.
     *
     * @return unit of time for time fields for orders (shelf life and decay duration)
     */
    @Override
    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    /**
     * Sets the time unit associated with any time field for the order, such as decay duration.
     *
     * @param timeUnit time unit to set for order, such as SECONDS or MILLISECONDS
     */
    public void setTimeUnit(TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
    }

    /**
     * Returns the current decay rate of the order based on the current shelf it was added to.
     *
     * @return current decay rate of order based on the current shelf it was added to
     */
    public float getCurrentDecayRate() {
        return currentDecayRate;
    }

    /**
     * Returns the date the order was added to its current shelf.
     *
     * @return date the order was added to its current shelf
     */
    public Date getAddedToShelfDate() {
        return addedToShelfDate;
    }

    /**
     * Since an order can move between shelves over its lifetime, the adjusted shelf life takes into account how
     * long the order has already aged on previous shelves (if any) since it was initialized. The adjusted shelf life
     * uses <code></code>getAddedToShelfDate()</code> as its time reference point.
     *
     * @return adjusted shelf life
     */
    public int getAdjustedShelfLife() {
        return adjustedShelfLife;
    }

    /**
     * Returns decay value as calculated by the food order decay formula.
     *
     * @param currentDate date from which to calculate order age with respect to order birth date
     * @return decay value as calculated by the food order decay formula
     */
    @Override
    public float getDecayValue(Date currentDate) {
        checkForNotInitializedException();

        int orderAge = (int) (getTimeUnit().convert(currentDate.getTime(), TimeUnit.MILLISECONDS) -
                getTimeUnit().convert(addedToShelfDate.getTime(), TimeUnit.MILLISECONDS));

        return decayFormula.getDecayValue(adjustedShelfLife, currentDecayRate, orderAge);
    }

    /**
     * Returns normalized decay value as calculated by the food order decay formula, which is the decay value divided
     * by the shelf life.
     *
     * @param currentDate date from which to calculate order age with respect to order birth date
     * @return normalized decay value
     */
    @Override
    public float getNormalizedDecayValue(Date currentDate) {
        checkForNotInitializedException();

        return getDecayValue(currentDate) / getShelfLife();
    }

    /**
     * Returns decay duration, which is the number of time units remaining before the decay value reaches a value of
     * zero.
     *
     * @param currentDate current date
     * @return decay duration in time units
     */
    @Override
    public float getDecayDuration(Date currentDate) {
        checkForNotInitializedException();

        int orderAge = (int) (getTimeUnit().convert(currentDate.getTime(), TimeUnit.MILLISECONDS) -
                getTimeUnit().convert(addedToShelfDate.getTime(), TimeUnit.MILLISECONDS));

        return Math.max(decayFormula.getDecayDuration(adjustedShelfLife, currentDecayRate) - orderAge, 0f);
    }

    /**
     * Since shelves can have affect the decay rate (e.g. orders on the overflow shelf decay at twice the rate than
     * normal temperature-specific shelves), this method is invoked at the time the order is placed on a shelf.
     *
     * @param currentDate date at which order is placed on a shelf with a given decay rate
     * @param decayRate new decay rate for order, which is usually a multiple of the order's original decay rate
     */
    @Override
    public void updateDecayRate(Date currentDate, float decayRate) {
        // set the adjusted shelf life to the current decay value
        this.adjustedShelfLife = (int) getDecayValue(currentDate);
        this.addedToShelfDate = currentDate;
        this.currentDecayRate = decayRate;
    }

    /**
     * Overrides <code>equals()</code> method for checking object equality.
     *
     * @param o object to compare against
     * @return true if objects are equal to each other
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return Objects.equals(id, order.getId());
    }

    /**
     * Overrides <code>hashCode()</code> function so that this object can be used in Sets and Maps
     *
     * @return hash code for Order object
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /**
     * Public version of the clone method for the Cloneable interface.
     *
     * @return cloned order object
     * @throws CloneNotSupportedException
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    /**
     * Helper method that makes sure order was properly initialized with internal state to do necessary calculations.
     */
    private void checkForNotInitializedException() {
        if (addedToShelfDate == null) {
            throw new NotInitializedException("initialize() method must be invoked first");
        }
    }

}
