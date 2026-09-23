import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for recommendation ranking.
 */
public class RecommendationsTest {
    private Profile profile;
    private Restaurant cellar;
    private Restaurant burger;
    private Restaurant tacos;
    private ArrayList<Restaurant> restaurants;
    private Recommendations recs;

    @BeforeEach
    public void setUp() {
        profile = new Profile("Sam", 24, "Blacksburg");
        cellar = new Restaurant("The Cellar", tags("Italian"));
        burger = new Restaurant("Burgers", tags("American"));
        tacos = new Restaurant("Taco Bell", tags("Mexican"));
        restaurants = new ArrayList<>(Arrays.asList(burger, tacos, cellar));
        recs = new Recommendations();
    }

    @Test
    public void profilePrefersMatchingRestaurants() {
        profile.addTasteTag(new Tag("Italian"));
        profile.addFavoriteCuisine("Italian");

        ArrayList<Restaurant> ranked = recs.rankRestaurants(profile, restaurants);

        assertTrue(ranked.get(0).getName().equals("The Cellar"));
        assertFalse(ranked.get(0).getName().equals("Burgers"));
    }

    @Test
    public void goodRatingsMoveACuisineUp() {
        profile.addTagWeight(new Tag("Mexican"), 1.0);
        assertEquals(tacos, recs.rankRestaurants(profile, restaurants).get(0));
    }

    @Test
    public void badRatingsMoveACuisineDown() {
        profile.addTagWeight(new Tag("American"), -1.0);
        ArrayList<Restaurant> ranked = recs.rankRestaurants(profile, restaurants);
        assertEquals(burger, ranked.get(ranked.size() - 1));
    }

    @Test
    public void favoriteRestaurantComesFirst() {
        profile.addTasteTag(new Tag("Italian"));
        profile.addFavoriteRestaurant(burger);
        assertEquals(burger, recs.rankRestaurants(profile, restaurants).get(0));
    }

    @Test
    public void blacklistedRestaurantsAreLeftOut() {
        profile.addTasteTag(new Tag("Italian"));
        profile.addToBlacklist(cellar);
        ArrayList<Restaurant> ranked = recs.rankRestaurants(profile, restaurants);
        assertFalse(ranked.contains(cellar));
        assertEquals(2, ranked.size());
    }

    @Test
    public void nullProfileIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> recs.rankRestaurants(null, restaurants));
    }

    @Test
    public void emptyOrNullListGivesNoRecommendations() {
        assertTrue(recs.rankRestaurants(profile, null).isEmpty());
        assertTrue(recs.rankRestaurants(profile, new ArrayList<>()).isEmpty());
    }

    @Test
    public void matchScoreIsHigherForAMatch() {
        profile.addTasteTag(new Tag("Italian"));
        assertTrue(recs.matchScore(cellar, profile) > recs.matchScore(burger, profile));
    }

    @Test
    public void matchScoreOfNullsIsZero() {
        assertEquals(0.0, recs.matchScore(null, profile));
        assertEquals(0.0, recs.matchScore(cellar, null));
    }

    private static ArrayList<Tag> tags(String... names) {
        ArrayList<Tag> tags = new ArrayList<>();
        for (String name : names) {
            tags.add(new Tag(name));
        }
        return tags;
    }
}
