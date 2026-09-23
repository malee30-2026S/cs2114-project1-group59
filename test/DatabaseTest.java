import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for Database. Each test gets a fresh database file in a temporary
 * folder that JUnit deletes afterward.
 *
 * @author Jaidev Gogineni
 */
public class DatabaseTest {
    private static final String EMAIL = "jaidevg@vt.edu";

    @TempDir
    Path folder;

    private String dbPath;
    private Database db;
    private Restaurant chilis;
    private Restaurant subway;

    @BeforeEach
    public void setUp() {
        dbPath = folder.resolve("test.db").toString();
        db = new Database(dbPath);
        chilis = new Restaurant("Chili's", tags("American", "Mexican"), 2, 37.1400, -80.4050, "1 Test Rd");
        chilis.addReview("Good chips");
        chilis.addMenuItem("Fajitas");
        subway = new Restaurant("Subway", tags("Sandwiches"), 1, 37.2296, -80.4139, "2 Test Rd");
        db.saveRestaurant(chilis);
        db.saveRestaurant(subway);
    }

    @AfterEach
    public void tearDown() {
        db.close();
    }

    // ------------------------------------------------------------ constructor

    @Test
    public void dataSurvivesClosingAndReopening() {
        db.close();
        db = new Database(dbPath);
        assertNotNull(db.findRestaurant("Subway"));
    }

    @Test
    public void blankPathIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Database(" "));
    }

    // ------------------------------------------------------------ saveRestaurant / findRestaurant

    @Test
    public void savedRestaurantComesBackWithEverything() {
        Restaurant found = db.findRestaurant("chili's");
        assertNotNull(found);
        assertEquals(tags("American", "Mexican"), found.getTags());
        assertEquals(2, found.getPriceLevel());
        assertEquals(37.1400, found.getLatitude());
        assertEquals(Arrays.asList("Good chips"), found.getReviews());
        assertEquals(Arrays.asList("Fajitas"), found.getMenu());
    }

    @Test
    public void savingAgainUpdatesInsteadOfDuplicating() {
        chilis.addTag(new Tag("Bar"));
        db.saveRestaurant(chilis);
        assertEquals(2, db.getAllRestaurants().size());
        assertTrue(db.findRestaurant("Chili's").hasTag(new Tag("bar")));
    }

    @Test
    public void saveNullIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> db.saveRestaurant(null));
    }

    @Test
    public void findMissingOrBlankReturnsNull() {
        assertNull(db.findRestaurant("none"));
        assertNull(db.findRestaurant(""));
        assertNull(db.findRestaurant(null));
    }

    @Test
    public void sqlInjectionFindsNothingAndBreaksNothing() {
        assertNull(db.findRestaurant("x'; DROP TABLE restaurants; --"));
        assertEquals(2, db.getAllRestaurants().size());
    }

    // ------------------------------------------------------------ getAllRestaurants / getAllTags

    @Test
    public void getAllRestaurantsIsSortedByName() {
        ArrayList<Restaurant> all = db.getAllRestaurants();
        assertEquals("Chili's", all.get(0).getName());
        assertEquals("Subway", all.get(1).getName());
    }

    @Test
    public void getAllTagsListsEveryTag() {
        assertEquals(tags("American", "Mexican", "Sandwiches"), db.getAllTags());
    }

    // ------------------------------------------------------------ getRestaurantsByTag

    @Test
    public void getRestaurantsByTagIgnoresCase() {
        assertEquals(Arrays.asList(chilis), db.getRestaurantsByTag(new Tag("MEXICAN")));
    }

    @Test
    public void getRestaurantsByNullTagIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> db.getRestaurantsByTag(null));
    }

    // ------------------------------------------------------------ removeRestaurant

    @Test
    public void removeRestaurantDeletesIt() {
        assertTrue(db.removeRestaurant(chilis));
        assertNull(db.findRestaurant("Chili's"));
        assertFalse(db.removeRestaurant(chilis));
    }

    @Test
    public void removeNullIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> db.removeRestaurant(null));
    }

    // ------------------------------------------------------------ profiles

    @Test
    public void savedProfileCanBeFoundIgnoringEmailCase() {
        db.saveProfile("jaidev", EMAIL);
        assertTrue(db.profileExists("JAIDEVG@vt.edu"));
        assertEquals("jaidev", db.getProfileName(EMAIL));
    }

    @Test
    public void blankProfileIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> db.saveProfile("", ""));
        assertFalse(db.profileExists(""));
    }

    // ------------------------------------------------------------ tag weights

    @Test
    public void tagWeightsAddUp() {
        db.saveProfile("jaidev", EMAIL);
        db.adjustTagWeight(EMAIL, new Tag("Italian"), 2.0);
        db.adjustTagWeight(EMAIL, new Tag("italian"), 1.5);
        db.adjustTagWeight(EMAIL, new Tag("Sushi"), -1.0);
        HashMap<Tag, Double> weights = db.getTagWeights(EMAIL);
        assertEquals(3.5, weights.get(new Tag("Italian")));
        assertEquals(-1.0, weights.get(new Tag("Sushi")));
    }

    @Test
    public void badTagWeightChangesAreRejected() {
        db.saveProfile("jaidev", EMAIL);
        assertThrows(IllegalArgumentException.class,
                () -> db.adjustTagWeight("nobody@vt.edu", new Tag("Italian"), 1));
        assertThrows(IllegalArgumentException.class,
                () -> db.adjustTagWeight(EMAIL, new Tag("Italian"), Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> db.adjustTagWeight(EMAIL, null, 1));
        assertTrue(db.getTagWeights("nobody@vt.edu").isEmpty());
    }

    private static ArrayList<Tag> tags(String... names) {
        ArrayList<Tag> tags = new ArrayList<>();
        for (String name : names) {
            tags.add(new Tag(name));
        }
        return tags;
    }
}
