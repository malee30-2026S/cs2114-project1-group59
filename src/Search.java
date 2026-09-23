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

    /**
     * Which cuisines usually serve a dish: {dish, cuisine, cuisine, ...}.
     * The dataset has no menus yet, so dish search goes through cuisines.
     */
    private static final String[][] DISH_CUISINES = {
        {"taco", "Mexican"}, {"burrito", "Mexican"}, {"quesadilla", "Mexican"}, {"nacho", "Mexican"},
        {"enchilada", "Mexican"}, {"fajita", "Mexican"}, {"churro", "Mexican"},
        {"pizza", "Pizza", "Italian"}, {"calzone", "Pizza", "Italian"}, {"pasta", "Italian"},
        {"spaghetti", "Italian"}, {"lasagna", "Italian"}, {"fettuccine", "Italian"}, {"alfredo", "Italian"},
        {"ravioli", "Italian"}, {"risotto", "Italian"},
        {"sushi", "Sushi", "Japanese"}, {"sashimi", "Sushi", "Japanese"}, {"ramen", "Ramen", "Japanese"},
        {"tempura", "Japanese"}, {"teriyaki", "Japanese"}, {"udon", "Japanese"},
        {"burger", "Burgers", "American"}, {"cheeseburger", "Burgers", "American"}, {"fries", "Burgers", "American"},
        {"hot dog", "American"}, {"wings", "American"}, {"milkshake", "Burgers", "American"},
        {"sandwich", "Sandwiches"}, {"sub", "Sandwiches"}, {"wrap", "Sandwiches", "Mediterranean"},
        {"curry", "Indian", "Nepalese", "Thai"}, {"tikka", "Indian"}, {"naan", "Indian"}, {"biryani", "Indian"},
        {"samosa", "Indian", "Nepalese"}, {"momo", "Nepalese"},
        {"pad thai", "Thai"}, {"pad see ew", "Thai"}, {"tom yum", "Thai"},
        {"fried rice", "Chinese", "Thai"}, {"lo mein", "Chinese"}, {"orange chicken", "Chinese"},
        {"general tso", "Chinese"}, {"dumpling", "Chinese", "Nepalese"}, {"egg roll", "Chinese"},
        {"noodle", "Chinese", "Japanese", "Thai", "Ramen"},
        {"gyro", "Greek", "Mediterranean"}, {"souvlaki", "Greek", "Mediterranean"}, {"falafel", "Mediterranean"},
        {"hummus", "Mediterranean"}, {"kebab", "Mediterranean"}, {"shawarma", "Mediterranean"},
        {"brisket", "BBQ"}, {"ribs", "BBQ"}, {"pulled pork", "BBQ"}, {"barbecue", "BBQ"}, {"bbq", "BBQ"},
        {"fish", "Seafood"}, {"shrimp", "Seafood"}, {"crab", "Seafood"}, {"oyster", "Seafood"},
    };

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

    /**
     * Finds places that serve a dish, like "tacos" or "pad thai".
     * Restaurants with the dish on their menu come first, then restaurants
     * whose cuisine usually serves it (tacos: Mexican places).
     *
     * @param dish what the user is craving
     * @return matching restaurants (empty if the dish isn't known)
     * @throws IllegalArgumentException if dish is null or blank
     */
    public ArrayList<Restaurant> searchByDish(String dish) {
        if (dish == null || dish.isBlank()) {
            throw new IllegalArgumentException("Enter a dish to search for");
        }
        String query = dish.trim().toLowerCase(Locale.ROOT);
        ArrayList<Restaurant> onMenu = new ArrayList<>();
        ArrayList<Restaurant> byCuisine = new ArrayList<>();
        ArrayList<Tag> cuisines = new ArrayList<>();
        for (String cuisine : cuisinesForDish(dish)) {
            cuisines.add(new Tag(cuisine));
        }
        for (Restaurant r : visibleRestaurants()) {
            if (servesOnMenu(r, query)) {
                onMenu.add(r);
            }
            else if (hasAny(r, cuisines)) {
                byCuisine.add(r);
            }
        }
        onMenu.sort(BY_NAME);
        byCuisine.sort(BY_NAME);
        onMenu.addAll(byCuisine);
        return onMenu;
    }

    /**
     * @param dish a dish name, e.g. "Tacos" or "chicken tikka masala"
     * @return the cuisines that usually serve it, e.g. ["Mexican"], or an
     *         empty list if the dish isn't known
     */
    public static ArrayList<String> cuisinesForDish(String dish) {
        ArrayList<String> cuisines = new ArrayList<>();
        if (dish == null || dish.isBlank()) {
            return cuisines;
        }
        String query = dish.trim().toLowerCase(Locale.ROOT);
        for (String[] entry : DISH_CUISINES) {
            if (query.contains(entry[0])) {
                for (int i = 1; i < entry.length; i++) {
                    if (!cuisines.contains(entry[i])) {
                        cuisines.add(entry[i]);
                    }
                }
            }
        }
        return cuisines;
    }

    private static boolean servesOnMenu(Restaurant r, String query) {
        for (String item : r.getMenu()) {
            if (item.toLowerCase(Locale.ROOT).contains(query)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasAny(Restaurant r, ArrayList<Tag> tags) {
        for (Tag tag : tags) {
            if (r.hasTag(tag)) {
                return true;
            }
        }
        return false;
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
