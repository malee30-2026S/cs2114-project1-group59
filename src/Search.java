import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;

/**
 * Finds restaurants by name, tag, price, or distance. If a blacklist is
 * given, every search skips the restaurants it blocks.
 */
public class Search {
    private static final Comparator<Restaurant> BY_NAME =
            Comparator.comparing(r -> r.getName().toLowerCase(Locale.ROOT));

    private final ArrayList<Restaurant> restaurants;
    private Blacklist blacklist;

    /**
     * Creates a search over a list of restaurants.
     *
     * @param restaurants the restaurants to search
     * @throws IllegalArgumentException if restaurants is null
     */
    public Search(ArrayList<Restaurant> restaurants) {
        this(restaurants, null);
    }

    /**
     * Creates a search that hides blacklisted restaurants.
     *
     * @param restaurants the restaurants to search
     * @param blacklist   the user's blacklist, or null for none
     * @throws IllegalArgumentException if restaurants is null
     */
    public Search(ArrayList<Restaurant> restaurants, Blacklist blacklist) {
        if (restaurants == null) {
            throw new IllegalArgumentException("Restaurant list cannot be null");
        }
        this.restaurants = new ArrayList<>();
        for (Restaurant r : restaurants) {
            if (r != null) {
                this.restaurants.add(r);
            }
        }
        this.blacklist = blacklist;
    }

    /**
     * @param blacklist the user's blacklist, or null for none
     */
    public void setBlacklist(Blacklist blacklist) {
        this.blacklist = blacklist;
    }

    /**
     * Finds restaurants whose name contains the text, ignoring case, so
     * "chil" finds Chili's.
     *
     * @param name all or part of a restaurant's name
     * @return matching restaurants, sorted by name
     * @throws IllegalArgumentException if name is null or blank
     */
    public ArrayList<Restaurant> searchByName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Enter a restaurant name to search for");
        }
        String query = name.trim().toLowerCase(Locale.ROOT);
        ArrayList<Restaurant> results = new ArrayList<>();
        for (Restaurant r : visibleRestaurants()) {
            if (r.getName().toLowerCase(Locale.ROOT).contains(query)) {
                results.add(r);
            }
        }
        results.sort(BY_NAME);
        return results;
    }

    /**
     * @param tag the tag to look for, e.g. Mexican
     * @return restaurants with that tag, sorted by name
     * @throws IllegalArgumentException if tag is null
     */
    public ArrayList<Restaurant> searchByTag(Tag tag) {
        if (tag == null) {
            throw new IllegalArgumentException("Tag cannot be null");
        }
        ArrayList<Restaurant> results = new ArrayList<>();
        for (Restaurant r : visibleRestaurants()) {
            if (r.hasTag(tag)) {
                results.add(r);
            }
        }
        results.sort(BY_NAME);
        return results;
    }

    /**
     * Finds restaurants at or under a price level. Restaurants with an
     * unknown price are left out, since we can't say they fit the budget.
     *
     * @param maxPrice 1 ($) through 4 ($$$$)
     * @return matching restaurants, cheapest first
     * @throws IllegalArgumentException if maxPrice isn't 1 to 4
     */
    public ArrayList<Restaurant> searchByPrice(int maxPrice) {
        if (maxPrice < 1 || maxPrice > Restaurant.MAX_PRICE_LEVEL) {
            throw new IllegalArgumentException(
                    "Max price must be 1 ($) to " + Restaurant.MAX_PRICE_LEVEL + " ($$$$)");
        }
        ArrayList<Restaurant> results = new ArrayList<>();
        for (Restaurant r : visibleRestaurants()) {
            if (r.getPriceLevel() != Restaurant.PRICE_UNKNOWN && r.getPriceLevel() <= maxPrice) {
                results.add(r);
            }
        }
        results.sort(Comparator.comparingInt(Restaurant::getPriceLevel).thenComparing(BY_NAME));
        return results;
    }

    /**
     * Finds restaurants within a distance of the user. Distances must be
     * filled in first with DistanceCalculator.updateDistances; restaurants
     * with no distance are left out.
     *
     * @param distance the farthest distance in miles
     * @return matching restaurants, closest first
     * @throws IllegalArgumentException if distance is negative, NaN, or
     *                                  unreasonably large
     */
    public ArrayList<Restaurant> searchByDistance(double distance) {
        if (Double.isNaN(distance) || distance < 0 || distance >= Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Distance must be a positive number of miles");
        }
        ArrayList<Restaurant> results = new ArrayList<>();
        for (Restaurant r : visibleRestaurants()) {
            if (!Double.isNaN(r.getDistance()) && r.getDistance() <= distance) {
                results.add(r);
            }
        }
        results.sort(Comparator.comparingDouble(Restaurant::getDistance).thenComparing(BY_NAME));
        return results;
    }

    /** Every restaurant the blacklist doesn't block. */
    private ArrayList<Restaurant> visibleRestaurants() {
        ArrayList<Restaurant> visible = new ArrayList<>();
        for (Restaurant r : restaurants) {
            if (blacklist == null || !blacklist.isBlocked(r)) {
                visible.add(r);
            }
        }
        return visible;
    }
}
