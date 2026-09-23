import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    // ------------------------------------------------------------ Profile bad input

    @Test
    public void profileRejectsEmptyOrNullValues() {
        Profile profile = new Profile("Ada", 22, "Blacksburg");
        assertThrows(IllegalArgumentException.class, () -> profile.addDietaryRestriction(" "));
        assertThrows(IllegalArgumentException.class, () -> profile.addFavoriteCuisine(null));
        assertThrows(IllegalArgumentException.class, () -> profile.addFavoriteRestaurant(null));
        assertThrows(IllegalArgumentException.class, () -> profile.addTasteTag(null));
        assertThrows(IllegalArgumentException.class, () -> profile.addTagWeight(new Tag("Italian"), Double.NaN));
    }

    @Test
    public void tagWeightsAddUp() {
        Profile profile = new Profile("Ada");
        profile.addTagWeight(new Tag("Italian"), 1.0);
        profile.addTagWeight(new Tag("italian"), 0.5);
        assertEquals(1.5, profile.getTagWeights().get(new Tag("Italian")));
    }

    @Test
    public void blacklistBlocksRestaurantsAndTags() {
        Profile profile = new Profile("Ada");
        Restaurant cellar = new Restaurant("The Cellar", new ArrayList<>());
        profile.addToBlacklist(cellar);
        assertTrue(profile.isBlacklisted(cellar));
        assertThrows(IllegalArgumentException.class, () -> profile.addToBlacklist(null));
    }

    @Test
    public void profileIsCompleteOnlyWithNameAndLocation() {
        assertTrue(new Profile("Ada", 22, "Blacksburg").isComplete());
        assertFalse(new Profile().isComplete());
    }

    // ------------------------------------------------------------ Survey

    @Test
    public void surveyCarriesEmailIntoProfileAndBack() {
        Survey survey = new Survey("Sam", 24, "Blacksburg");
        survey.setEmail("sam@vt.edu");
        Profile profile = survey.toProfile();
        assertEquals("sam@vt.edu", profile.getEmail());

        Survey reloaded = new Survey();
        reloaded.loadFromProfile(profile);
        assertEquals("sam@vt.edu", reloaded.getEmail());
        assertEquals("Sam", reloaded.getName());
    }

    @Test
    public void surveyRejectsBadAnswers() {
        Survey survey = new Survey();
        assertThrows(IllegalArgumentException.class, () -> survey.addTag(""));
        assertThrows(IllegalArgumentException.class, () -> survey.addTag((Tag) null));
        assertThrows(IllegalArgumentException.class, () -> survey.addDietaryRestriction(null));
        assertThrows(IllegalArgumentException.class, () -> survey.loadFromProfile(null));
    }

    // ------------------------------------------------------------ Data

    @Test
    public void areaDataHasPricesAndLocations() {
        Data data = Data.loadYelpAreaData();
        Restaurant mellow = null;
        for (Restaurant r : data.getRestaurants()) {
            if (r.getName().equals("Mellow Mushroom")) {
                mellow = r;
            }
        }
        assertTrue(data.getRestaurants().size() >= 20);
        assertTrue(mellow != null && mellow.hasLocation());
        assertEquals(2, mellow.getPriceLevel());
        assertTrue(data.getTags().contains(new Tag("Thai")));
    }

    @Test
    public void dataRejectsOrSkipsBadRows() {
        Data data = new Data();
        assertThrows(IllegalArgumentException.class, () -> data.createTag(" "));
        assertThrows(IllegalArgumentException.class, () -> data.addRestaurant(null));
        assertThrows(IllegalArgumentException.class, () -> data.getHoursOpen(null));

        ArrayList<String[]> rows = new ArrayList<>();
        rows.add(null);
        rows.add(new String[] {""});
        rows.add(new String[] {"Real Place", "Thai", "9-5", "Good"});
        assertEquals(1, data.parseYelpData(rows).size());
        assertTrue(data.parseYelpData(null).isEmpty());
    }
}
