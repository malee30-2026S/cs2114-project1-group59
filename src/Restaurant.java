import java.util.ArrayList;
import java.util.Locale;

/**
 * One restaurant in the Blacksburg/Christiansburg area. Search and
 * Recommendations use these objects when filtering and ranking results.
 *
 * Price level follows the $ scale from the scope doc: 1 = $, 2 = $$,
 * 3 = $$$, 4 = $$$$, and 0 means unknown.
 *
 * Distance is in miles from the user. It starts as Double.NaN (unknown)
 * until a DistanceCalculator fills it in, so a "within 5 miles" check
 * like getDistance() <= 5 is false for restaurants with no distance yet.
 *
 * @author Jaidev Gogineni
 */
public class Restaurant {
    /** Price level used when the price isn't known. */
    public static final int PRICE_UNKNOWN = 0;
    /** Highest price level ($$$$). */
    public static final int MAX_PRICE_LEVEL = 4;

    private final String name;
    private final ArrayList<Tag> tags;
    private final ArrayList<String> reviews;
    private final ArrayList<String> menu;
    private String address;
    private int priceLevel;
    private double latitude;
    private double longitude;
    private double distance;

    /**
     * Creates a restaurant with just a name and tags. Price, location, and
     * address can be filled in later with the setters.
     *
     * @param name the restaurant's name
     * @param tags the restaurant's tags (may be empty, not null)
     * @throws IllegalArgumentException if name is blank or tags is null or
     *                                  contains null
     */
    public Restaurant(String name, ArrayList<Tag> tags) {
        this(name, tags, PRICE_UNKNOWN, Double.NaN, Double.NaN, "");
    }

    /**
     * Creates a restaurant with all of its core details.
     *
     * @param name       the restaurant's name
     * @param tags       the restaurant's tags (may be empty, not null)
     * @param priceLevel 1 ($) through 4 ($$$$), or 0 if unknown
     * @param latitude   GPS latitude, or Double.NaN if unknown
     * @param longitude  GPS longitude, or Double.NaN if unknown
     * @param address    street address, or "" if unknown
     * @throws IllegalArgumentException if any value is invalid
     */
    public Restaurant(String name, ArrayList<Tag> tags, int priceLevel,
            double latitude, double longitude, String address) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Restaurant name cannot be empty");
        }
        if (tags == null) {
            throw new IllegalArgumentException("Tags cannot be null (use an empty list)");
        }
        this.name = name.trim();
        this.tags = new ArrayList<>();
        for (Tag tag : tags) {
            addTag(tag);
        }
        this.reviews = new ArrayList<>();
        this.menu = new ArrayList<>();
        this.distance = Double.NaN;
        setPriceLevel(priceLevel);
        setLocation(latitude, longitude);
        setAddress(address);
    }

    /**
     * @return the restaurant's name
     */
    public String getName() {
        return name;
    }

    /**
     * @return a copy of the restaurant's tags (use addTag to change them)
     */
    public ArrayList<Tag> getTags() {
        return new ArrayList<>(tags);
    }

    /**
     * Adds a tag. Adding a tag the restaurant already has does nothing.
     *
     * @param tag the tag to add
     * @throws IllegalArgumentException if tag is null
     */
    public void addTag(Tag tag) {
        if (tag == null) {
            throw new IllegalArgumentException("Tag cannot be null");
        }
        if (!tags.contains(tag)) {
            tags.add(tag);
        }
    }

    /**
     * @param tag the tag to look for
     * @return true if this restaurant has the tag
     */
    public boolean hasTag(Tag tag) {
        return tags.contains(tag);
    }

    /**
     * @return the distance from the user in miles, or Double.NaN if it
     *         hasn't been calculated yet
     */
    public double getDistance() {
        return distance;
    }

    /**
     * Stores the distance from the user. Normally called by
     * DistanceCalculator rather than by hand.
     *
     * @param miles distance in miles
     * @throws IllegalArgumentException if miles is negative or not a number
     */
    public void setDistance(double miles) {
        if (Double.isNaN(miles) || Double.isInfinite(miles) || miles < 0) {
            throw new IllegalArgumentException("Distance must be a non-negative number");
        }
        this.distance = miles;
    }

    /**
     * Marks the distance as unknown again, e.g. when the user picks a
     * location we don't have coordinates for.
     */
    public void clearDistance() {
        this.distance = Double.NaN;
    }

    /**
     * @return 1 ($) through 4 ($$$$), or 0 if unknown
     */
    public int getPriceLevel() {
        return priceLevel;
    }

    /**
     * @param priceLevel 1 ($) through 4 ($$$$), or 0 if unknown
     * @throws IllegalArgumentException if priceLevel is outside 0-4
     */
    public void setPriceLevel(int priceLevel) {
        if (priceLevel < PRICE_UNKNOWN || priceLevel > MAX_PRICE_LEVEL) {
            throw new IllegalArgumentException(
                    "Price level must be 0 (unknown) to " + MAX_PRICE_LEVEL);
        }
        this.priceLevel = priceLevel;
    }

    /**
     * @return the price level as dollar signs, e.g. "$$", or "?" if unknown
     */
    public String getPriceSymbol() {
        return priceLevel == PRICE_UNKNOWN ? "?" : "$".repeat(priceLevel);
    }

    /**
     * @return GPS latitude, or Double.NaN if unknown
     */
    public double getLatitude() {
        return latitude;
    }

    /**
     * @return GPS longitude, or Double.NaN if unknown
     */
    public double getLongitude() {
        return longitude;
    }

    /**
     * @return true if the restaurant's GPS location is known
     */
    public boolean hasLocation() {
        return !Double.isNaN(latitude) && !Double.isNaN(longitude);
    }

    /**
     * Sets the GPS location. Pass Double.NaN for both to mark it unknown.
     *
     * @param latitude  -90 to 90
     * @param longitude -180 to 180
     * @throws IllegalArgumentException if the coordinates are out of range,
     *                                  or only one of them is NaN
     */
    public void setLocation(double latitude, double longitude) {
        if (Double.isNaN(latitude) != Double.isNaN(longitude)) {
            throw new IllegalArgumentException("Latitude and longitude must both be set or both be NaN");
        }
        if (!Double.isNaN(latitude)) {
            DistanceCalculator.checkCoordinates(latitude, longitude);
        }
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /**
     * @return the street address, or "" if unknown
     */
    public String getAddress() {
        return address;
    }

    /**
     * @param address street address; null is treated as unknown ("")
     */
    public void setAddress(String address) {
        this.address = address == null ? "" : address.trim();
    }

    /**
     * @return a copy of the restaurant's reviews
     */
    public ArrayList<String> getReviews() {
        return new ArrayList<>(reviews);
    }

    /**
     * Adds a review. Moderator should check the text before it gets here.
     *
     * @param review the review text
     * @throws IllegalArgumentException if review is null or blank
     */
    public void addReview(String review) {
        if (review == null || review.isBlank()) {
            throw new IllegalArgumentException("Review cannot be empty");
        }
        reviews.add(review.trim());
    }

    /**
     * @return a copy of the restaurant's menu items
     */
    public ArrayList<String> getMenu() {
        return new ArrayList<>(menu);
    }

    /**
     * @param item a dish or menu item, e.g. "Fettuccine Alfredo"
     * @throws IllegalArgumentException if item is null or blank
     */
    public void addMenuItem(String item) {
        if (item == null || item.isBlank()) {
            throw new IllegalArgumentException("Menu item cannot be empty");
        }
        menu.add(item.trim());
    }

    /**
     * Two restaurants are the same if they have the same name and address,
     * ignoring case. That keeps separate locations of a chain distinct.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Restaurant)) {
            return false;
        }
        Restaurant that = (Restaurant) other;
        return name.equalsIgnoreCase(that.name) && address.equalsIgnoreCase(that.address);
    }

    @Override
    public int hashCode() {
        return (name.toLowerCase(Locale.ROOT) + "|" + address.toLowerCase(Locale.ROOT)).hashCode();
    }

    @Override
    public String toString() {
        String miles = Double.isNaN(distance) ? "" : String.format(Locale.ROOT, ", %.1f mi", distance);
        return name + " (" + getPriceSymbol() + miles + ") " + tags;
    }
}
