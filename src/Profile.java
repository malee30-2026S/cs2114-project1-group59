import java.util.ArrayList;
import java.util.HashMap;

/**
 * Stores the user's profile information, including preferences, dietary
 * needs, favorite cuisines, favorite restaurants, and a running tag-weight
 * model used by recommendations.
 */
public class Profile {
    private String name;
    private String email;
    private int age;
    private String location;
    private final ArrayList<String> dietaryRestrictions;
    private final ArrayList<String> favoriteCuisines;
    private final ArrayList<Restaurant> favoriteRestaurants;
    private final ArrayList<Tag> tasteProfile;
    private final Blacklist blacklist;
    private final HashMap<Tag, Double> tagWeights;

    /**
     * Creates an empty profile.
     */
    public Profile() {
        this("", 0, "");
    }

    /**
     * Creates a profile with the basic user details already known.
     *
     * @param name the user's name
     * @param age the user's age
     * @param location the user's current city or area
     */
    public Profile(String name, int age, String location) {
        this.name = name == null ? "" : name.trim();
        this.email = "";
        this.age = age;
        this.location = location == null ? "" : location.trim();
        this.dietaryRestrictions = new ArrayList<>();
        this.favoriteCuisines = new ArrayList<>();
        this.favoriteRestaurants = new ArrayList<>();
        this.tasteProfile = new ArrayList<>();
        this.blacklist = new Blacklist();
        this.tagWeights = new HashMap<>();
    }

    /**
     * Creates a profile with only a name.
     *
     * @param name the user's name
     */
    public Profile(String name) {
        this(name, 0, "");
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null ? "" : name.trim();
    }

    /**
     * @return the user's email, which Database uses to save their ratings
     */
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email == null ? "" : email.trim();
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location == null ? "" : location.trim();
    }

    /**
     * @return true if enough information has been collected to create a usable
     *         profile.
     */
    public boolean isComplete() {
        return !name.isBlank() && !location.isBlank();
    }

    public void addDietaryRestriction(String restriction) {
        if (restriction == null || restriction.isBlank()) {
            throw new IllegalArgumentException("Dietary restriction cannot be empty");
        }
        String normalized = restriction.trim();
        if (!dietaryRestrictions.contains(normalized)) {
            dietaryRestrictions.add(normalized);
        }
    }

    public void removeDietaryRestriction(String restriction) {
        if (restriction == null) {
            throw new IllegalArgumentException("Dietary restriction cannot be null");
        }
        dietaryRestrictions.remove(restriction.trim());
    }

    public ArrayList<String> getDietaryRestrictions() {
        return new ArrayList<>(dietaryRestrictions);
    }

    public void addFavoriteCuisine(String cuisine) {
        if (cuisine == null || cuisine.isBlank()) {
            throw new IllegalArgumentException("Cuisine cannot be empty");
        }
        String normalized = cuisine.trim();
        if (!favoriteCuisines.contains(normalized)) {
            favoriteCuisines.add(normalized);
        }
    }

    public void removeFavoriteCuisine(String cuisine) {
        if (cuisine == null) {
            throw new IllegalArgumentException("Cuisine cannot be null");
        }
        favoriteCuisines.remove(cuisine.trim());
    }

    public ArrayList<String> getFavoriteCuisines() {
        return new ArrayList<>(favoriteCuisines);
    }

    public void addFavoriteRestaurant(Restaurant restaurant) {
        if (restaurant == null) {
            throw new IllegalArgumentException("Restaurant cannot be null");
        }
        if (!favoriteRestaurants.contains(restaurant)) {
            favoriteRestaurants.add(restaurant);
        }
    }

    public void removeFavoriteRestaurant(Restaurant restaurant) {
        if (restaurant == null) {
            throw new IllegalArgumentException("Restaurant cannot be null");
        }
        favoriteRestaurants.remove(restaurant);
    }

    public ArrayList<Restaurant> getFavoriteRestaurants() {
        return new ArrayList<>(favoriteRestaurants);
    }

    public void addTasteTag(Tag tag) {
        if (tag == null) {
            throw new IllegalArgumentException("Tag cannot be null");
        }
        if (!tasteProfile.contains(tag)) {
            tasteProfile.add(tag);
        }
    }

    public ArrayList<Tag> getTasteProfile() {
        return new ArrayList<>(tasteProfile);
    }

    public void addTagWeight(Tag tag, double weight) {
        if (tag == null) {
            throw new IllegalArgumentException("Tag cannot be null");
        }
        if (Double.isNaN(weight) || Double.isInfinite(weight)) {
            throw new IllegalArgumentException("Weight must be a finite number");
        }
        double current = tagWeights.getOrDefault(tag, 0.0);
        tagWeights.put(tag, current + weight);
    }

    public HashMap<Tag, Double> getTagWeights() {
        return new HashMap<>(tagWeights);
    }

    public Blacklist getBlacklist() {
        return blacklist;
    }

    public void addToBlacklist(Restaurant restaurant) {
        blacklist.addRestaurant(restaurant);
    }

    public void addToBlacklistTag(Tag tag) {
        blacklist.addTag(tag);
    }

    public boolean isBlacklisted(Restaurant restaurant) {
        return blacklist.isBlocked(restaurant);
    }

    @Override
    public String toString() {
        return "Profile{name='" + name + "', age=" + age + ", location='" + location
                + "', dietaryRestrictions=" + dietaryRestrictions
                + ", favoriteCuisines=" + favoriteCuisines
                + ", favoriteRestaurants=" + favoriteRestaurants + "}";
    }
}
