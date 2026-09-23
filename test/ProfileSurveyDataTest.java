import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import org.junit.jupiter.api.Test;

/**
 * Tests for the profile, survey, and data model classes.
 */
public class ProfileSurveyDataTest {
    @Test
    public void profileTracksUserInfoAndFavorites() {
        Profile profile = new Profile("Ada", 22, "Blacksburg");
        profile.addDietaryRestriction("Vegan");
        profile.addFavoriteCuisine("Italian");
        Restaurant favorite = new Restaurant("The Cellar", new ArrayList<>());
        profile.addFavoriteRestaurant(favorite);

        assertEquals("Ada", profile.getName());
        assertEquals(22, profile.getAge());
        assertEquals("Blacksburg", profile.getLocation());
        assertTrue(profile.getDietaryRestrictions().contains("Vegan"));
        assertTrue(profile.getFavoriteCuisines().contains("Italian"));
        assertTrue(profile.getFavoriteRestaurants().contains(favorite));
    }

    @Test
    public void surveyCreatesProfileFromAnswers() {
        Survey survey = new Survey();
        survey.setName("Sam");
        survey.setAge(24);
        survey.setLocation("Christiansburg");
        survey.addDietaryRestriction("Gluten-Free");
        survey.addTag(new Tag("Italian"));

        Profile profile = survey.toProfile();
        assertEquals("Sam", profile.getName());
        assertEquals(24, profile.getAge());
        assertEquals("Christiansburg", profile.getLocation());
        assertTrue(profile.getDietaryRestrictions().contains("Gluten-Free"));
        assertTrue(profile.getTasteProfile().contains(new Tag("Italian")));
    }

    @Test
    public void dataBuildsRestaurantsAndMetadata() {
        Data data = new Data();
        ArrayList<String[]> rows = new ArrayList<>();
        rows.add(new String[] {"The Cellar", "Italian", "Cozy", "11am-9pm", "Fresh pasta and pizza"});

        ArrayList<Restaurant> restaurants = data.parseYelpData(rows);

        assertEquals(1, restaurants.size());
        assertTrue(restaurants.get(0).hasTag(new Tag("Italian")));
        assertEquals("11am-9pm", data.getHoursOpen(restaurants.get(0)));
        assertEquals("Fresh pasta and pizza", data.getBio(restaurants.get(0)));
    }
}
