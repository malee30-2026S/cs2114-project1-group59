import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;

/**
 * Ranks restaurants by how well they match a user's profile.
 */
public class Recommendations {
    /**
     * Calculates a match score for a restaurant against a profile.
     * Higher scores are better.
     */
    public double matchScore(Restaurant restaurant, Profile profile) {
        if (restaurant == null || profile == null) {
            return 0.0;
        }

        double score = 0.0;
        HashSet<Tag> profileTags = new HashSet<>();

        for (Tag tag : profile.getTasteProfile()) {
            profileTags.add(tag);
        }
        for (String cuisine : profile.getFavoriteCuisines()) {
            profileTags.add(new Tag(cuisine));
        }

        for (Tag tag : restaurant.getTags()) {
            if (profileTags.contains(tag)) {
                score += 4.0;
            }
            if (profile.getDietaryRestrictions().contains("Vegan") && tag.getName().equalsIgnoreCase("vegan")) {
                score += 2.0;
            }
            if (profile.getDietaryRestrictions().contains("Vegetarian") && tag.getName().equalsIgnoreCase("vegetarian")) {
                score += 2.0;
            }
            if (profile.getDietaryRestrictions().contains("Gluten-Free") && tag.getName().equalsIgnoreCase("gluten-free")) {
                score += 2.0;
            }
        }

        for (Map.Entry<Tag, Double> entry : profile.getTagWeights().entrySet()) {
            if (restaurant.hasTag(entry.getKey())) {
                score += entry.getValue();
            }
        }

        if (profile.getFavoriteRestaurants().contains(restaurant)) {
            score += 5.0;
        }

        for (String cuisine : profile.getFavoriteCuisines()) {
            if (restaurant.hasTag(new Tag(cuisine))) {
                score += 2.0;
            }
        }

        if (!Double.isNaN(restaurant.getDistance()) && restaurant.getDistance() <= 5.0) {
            score += 1.5;
        }

        return score;
    }

    /**
     * Ranks restaurants from best match to worst.
     */
    public ArrayList<Restaurant> rankRestaurants(Profile profile, ArrayList<Restaurant> restaurants) {
        if (restaurants == null || restaurants.isEmpty()) {
            return new ArrayList<>();
        }

        ArrayList<Restaurant> ranked = new ArrayList<>(restaurants);
        ranked.sort(Comparator.comparingDouble((Restaurant r) -> matchScore(r, profile)).reversed());
        return ranked;
    }
}
