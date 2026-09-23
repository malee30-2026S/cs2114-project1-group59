import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * Stores the user's profile information, including preferences, dietary
 * needs, favorite cuisines, favorite restaurants, flavor profile, and a
 * running tag-weight model used by recommendations.
 */
public class Profile {
    /** The five flavors in the flavor profile. */
    public static final String[] FLAVORS = {"Sweet", "Salty", "Umami", "Sour", "Bitter"};
    /** Flavor levels go from 1 (not a fan) to 5 (love it). */
    public static final int MIN_FLAVOR_LEVEL = 1;
    public static final int MAX_FLAVOR_LEVEL = 5;
    /** Level every flavor starts at until the user says otherwise. */
    public static final int DEFAULT_FLAVOR_LEVEL = 3;

    private String name;
    private String email;
    private int age;
    private String location;
    private double latitude;
    private double longitude;
    private final LinkedHashMap<String, Integer> flavorProfile;
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
        this.latitude = Double.NaN;
        this.longitude = Double.NaN;
        this.flavorProfile = new LinkedHashMap<>();
        for (String flavor : FLAVORS) {
            flavorProfile.put(flavor, DEFAULT_FLAVOR_LEVEL);
        }
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
     * Stores the user's live GPS location. Only set this after the user
     * has allowed location access.
     *
     * @throws IllegalArgumentException if the coordinates aren't on Earth
     */
    public void setCoordinates(double latitude, double longitude) {
        DistanceCalculator.checkCoordinates(latitude, longitude);
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /**
     * Forgets the live location, e.g. when the user turns location off.
     */
    public void clearCoordinates() {
        this.latitude = Double.NaN;
        this.longitude = Double.NaN;
    }

    /**
     * @return true if the user's live location is known
     */
    public boolean hasCoordinates() {
        return !Double.isNaN(latitude) && !Double.isNaN(longitude);
    }

    /**
     * @return live latitude, or NaN if unknown
     */
    public double getLatitude() {
        return latitude;
    }

    /**
     * @return live longitude, or NaN if unknown
     */
    public double getLongitude() {
        return longitude;
    }

    /**
     * Sets how much the user likes a flavor.
     *
     * @param flavor one of FLAVORS, e.g. "Umami" (case doesn't matter)
     * @param level  1 (not a fan) to 5 (love it)
     * @throws IllegalArgumentException if the flavor or level is invalid
     */
    public void setFlavorLevel(String flavor, int level) {
        String key = checkFlavor(flavor);
        checkFlavorLevel(level);
        flavorProfile.put(key, level);
    }

    /**
     * @param flavor one of FLAVORS
     * @return how much the user likes it, 1 to 5
     * @throws IllegalArgumentException if the flavor is invalid
     */
    public int getFlavorLevel(String flavor) {
        return flavorProfile.get(checkFlavor(flavor));
    }

    /**
     * @return a copy of the flavor profile, in the order of FLAVORS
     */
    public LinkedHashMap<String, Integer> getFlavorProfile() {
        return new LinkedHashMap<>(flavorProfile);
    }

    /**
     * @param flavor a flavor name in any case, e.g. "umami"
     * @return its name as written in FLAVORS, e.g. "Umami"
     * @throws IllegalArgumentException if it isn't one of FLAVORS
     */
    public static String checkFlavor(String flavor) {
        if (flavor != null) {
            for (String known : FLAVORS) {
                if (known.equalsIgnoreCase(flavor.trim())) {
                    return known;
                }
            }
        }
        throw new IllegalArgumentException("Flavor must be one of Sweet, Salty, Umami, Sour, Bitter");
    }

    /**
     * @throws IllegalArgumentException if level isn't 1 to 5
     */
    public static void checkFlavorLevel(int level) {
        if (level < MIN_FLAVOR_LEVEL || level > MAX_FLAVOR_LEVEL) {
            throw new IllegalArgumentException("Flavor level must be " + MIN_FLAVOR_LEVEL
                    + " to " + MAX_FLAVOR_LEVEL);
        }
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
