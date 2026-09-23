import java.util.ArrayList;

/**
 * Restaurants and tags a user never wants to see. Search and
 * Recommendations skip anything on the blacklist.
 *
 * A restaurant is blocked if it's on the list itself, or if it has any
 * blacklisted tag. Blacklisting the tag "Sushi" hides every sushi place.
 */
public class Blacklist {
    private final ArrayList<Restaurant> restaurants;
    private final ArrayList<Tag> tags;

    /**
     * Creates an empty blacklist.
     */
    public Blacklist() {
        restaurants = new ArrayList<>();
        tags = new ArrayList<>();
    }

    /**
     * Adds a restaurant to the blacklist. Adding one that's already on it
     * does nothing.
     *
     * @param r the restaurant to block
     * @throws IllegalArgumentException if r is null
     */
    public void addRestaurant(Restaurant r) {
        if (r == null) {
            throw new IllegalArgumentException("Restaurant cannot be null");
        }
        if (!restaurants.contains(r)) {
            restaurants.add(r);
        }
    }

    /**
     * Removes a restaurant from the blacklist. Removing one that isn't on
     * it does nothing.
     *
     * @param r the restaurant to unblock
     * @throws IllegalArgumentException if r is null
     */
    public void removeRestaurant(Restaurant r) {
        if (r == null) {
            throw new IllegalArgumentException("Restaurant cannot be null");
        }
        restaurants.remove(r);
    }

    /**
     * @return a copy of the blacklisted restaurants
     */
    public ArrayList<Restaurant> getBlacklistedRestaurants() {
        return new ArrayList<>(restaurants);
    }

    /**
     * Blocks every restaurant with this tag, e.g. a cuisine the user hates.
     *
     * @param tag the tag to block
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
     * @param tag the tag to unblock
     * @throws IllegalArgumentException if tag is null
     */
    public void removeTag(Tag tag) {
        if (tag == null) {
            throw new IllegalArgumentException("Tag cannot be null");
        }
        tags.remove(tag);
    }

    /**
     * @return a copy of the blacklisted tags
     */
    public ArrayList<Tag> getBlacklistedTags() {
        return new ArrayList<>(tags);
    }

    /**
     * @param r the restaurant to check
     * @return true if r is blacklisted or has a blacklisted tag
     * @throws IllegalArgumentException if r is null
     */
    public boolean isBlocked(Restaurant r) {
        if (r == null) {
            throw new IllegalArgumentException("Restaurant cannot be null");
        }
        if (restaurants.contains(r)) {
            return true;
        }
        for (Tag tag : r.getTags()) {
            if (tags.contains(tag)) {
                return true;
            }
        }
        return false;
    }
}
