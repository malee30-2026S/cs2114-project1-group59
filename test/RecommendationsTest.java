import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import org.junit.jupiter.api.Test;

/**
 * Tests for recommendation ranking.
 */
public class RecommendationsTest {
    @Test
    public void profilePrefersMatchingRestaurants() {
        Profile profile = new Profile("Sam", 24, "Blacksburg");
        profile.addTasteTag(new Tag("Italian"));
        profile.addFavoriteCuisine("Italian");

        ArrayList<Tag> italian = new ArrayList<>();
        italian.add(new Tag("Italian"));
        Restaurant cellar = new Restaurant("The Cellar", italian);

        ArrayList<Tag> american = new ArrayList<>();
        american.add(new Tag("American"));
        Restaurant burger = new Restaurant("Burgers", american);

        Recommendations recs = new Recommendations();
        ArrayList<Restaurant> ranked = recs.rankRestaurants(profile, new ArrayList<Restaurant>() {{
            add(cellar);
            add(burger);
        }});

        assertTrue(ranked.get(0).getName().equals("The Cellar"));
        assertFalse(ranked.get(0).getName().equals("Burgers"));
    }
}
