import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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

    // ------------------------------------------------------------ saveProfile(Profile) / loadProfile

    @Test
    public void fullProfileRoundTrips() {
        Profile profile = new Profile("Jaidev", 20, "Blacksburg");
        profile.setEmail(EMAIL);
        profile.addDietaryRestriction("Vegetarian");
        profile.addFavoriteCuisine("Thai");
        profile.addTasteTag(new Tag("Italian"));
        profile.addTagWeight(new Tag("Pizza"), 1.5);
        profile.addFavoriteRestaurant(chilis);
        profile.addToBlacklist(subway);
        profile.addToBlacklistTag(new Tag("Sushi"));
        db.saveProfile(profile);

        Profile loaded = db.loadProfile("JAIDEVG@VT.EDU");
        assertNotNull(loaded);
        assertEquals("Jaidev", loaded.getName());
        assertEquals(20, loaded.getAge());
        assertEquals("Blacksburg", loaded.getLocation());
        assertEquals(Arrays.asList("Vegetarian"), loaded.getDietaryRestrictions());
        assertEquals(Arrays.asList("Thai"), loaded.getFavoriteCuisines());
        assertEquals(tags("Italian"), loaded.getTasteProfile());
        assertEquals(1.5, loaded.getTagWeights().get(new Tag("pizza")));
        assertTrue(loaded.getFavoriteRestaurants().contains(chilis));
        assertTrue(loaded.isBlacklisted(subway));
        assertTrue(loaded.getBlacklist().getBlacklistedTags().contains(new Tag("sushi")));
    }

    @Test
    public void flavorsAndLiveLocationRoundTrip() {
        Profile profile = new Profile("Jaidev", 20, "Current location");
        profile.setEmail(EMAIL);
        profile.setFlavorLevel("Umami", 5);
        profile.setCoordinates(37.23, -80.41);
        db.saveProfile(profile);

        Profile loaded = db.loadProfile(EMAIL);
        assertEquals(5, loaded.getFlavorLevel("Umami"));
        assertEquals(Profile.DEFAULT_FLAVOR_LEVEL, loaded.getFlavorLevel("Sweet"));
        assertTrue(loaded.hasCoordinates());
        assertEquals(37.23, loaded.getLatitude());

        profile.clearCoordinates();
        db.saveProfile(profile);
        assertFalse(db.loadProfile(EMAIL).hasCoordinates());
    }

    // ------------------------------------------------------------ photo

    @Test
    public void photoIsSavedAndRemoved() {
        db.saveProfile("jaidev", EMAIL);
        assertNull(db.loadPhoto(EMAIL));
        byte[] photo = {1, 2, 3, 4};
        db.savePhoto(EMAIL, photo);
        assertArrayEquals(photo, db.loadPhoto(EMAIL));

        Profile profile = db.loadProfile(EMAIL);
        db.saveProfile(profile);
        assertArrayEquals(photo, db.loadPhoto(EMAIL), "saving the profile keeps the photo");

        db.removePhoto(EMAIL);
        assertNull(db.loadPhoto(EMAIL));
    }

    @Test
    public void badPhotosAreRejected() {
        db.saveProfile("jaidev", EMAIL);
        assertThrows(IllegalArgumentException.class, () -> db.savePhoto(EMAIL, new byte[0]));
        assertThrows(IllegalArgumentException.class, () -> db.savePhoto(EMAIL, null));
        assertThrows(IllegalArgumentException.class, () -> db.savePhoto("nobody@vt.edu", new byte[] {1}));
        assertNull(db.loadPhoto(""));
    }

    // ------------------------------------------------------------ location permission

    @Test
    public void locationPermissionIsRemembered() {
        assertNull(db.getLocationPermission());
        db.setLocationPermission(true);
        db.close();
        db = new Database(dbPath);
        assertEquals(Boolean.TRUE, db.getLocationPermission());
        db.setLocationPermission(false);
        assertEquals(Boolean.FALSE, db.getLocationPermission());
    }

    @Test
    public void savingAgainReplacesOldLists() {
        Profile profile = new Profile("Jaidev", 20, "Blacksburg");
        profile.setEmail(EMAIL);
        profile.addToBlacklist(subway);
        db.saveProfile(profile);
        profile.getBlacklist().removeRestaurant(subway);
        db.saveProfile(profile);
        assertFalse(db.loadProfile(EMAIL).isBlacklisted(subway));
    }

    @Test
    public void loadMissingProfileReturnsNull() {
        assertNull(db.loadProfile("nobody@vt.edu"));
        assertNull(db.loadProfile(""));
        assertNull(db.loadProfile(null));
    }

    @Test
    public void badProfilesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> db.saveProfile((Profile) null));
        assertThrows(IllegalArgumentException.class, () -> db.saveProfile(new Profile("No Email")));
        Profile noName = new Profile();
        noName.setEmail(EMAIL);
        assertThrows(IllegalArgumentException.class, () -> db.saveProfile(noName));
    }

    // ------------------------------------------------------------ pending visits

    @Test
    public void pendingVisitsAreRememberedUntilRemoved() {
        db.saveProfile("jaidev", EMAIL);
        db.addPendingVisit(EMAIL, chilis);
        db.addPendingVisit(EMAIL, chilis);
        db.addPendingVisit(EMAIL, subway);
        assertEquals(Arrays.asList(chilis, subway), db.getPendingVisits(EMAIL));
        assertTrue(db.removePendingVisit(EMAIL, chilis));
        assertFalse(db.removePendingVisit(EMAIL, chilis));
        assertEquals(Arrays.asList(subway), db.getPendingVisits(EMAIL));
    }

    @Test
    public void pendingVisitsSurviveSavingTheProfile() {
        Profile profile = new Profile("Jaidev", 20, "Blacksburg");
        profile.setEmail(EMAIL);
        db.saveProfile(profile);
        db.addPendingVisit(EMAIL, chilis);
        db.saveProfile(profile);
        assertEquals(1, db.getPendingVisits(EMAIL).size());
    }

    @Test
    public void badPendingVisitsAreRejected() {
        db.saveProfile("jaidev", EMAIL);
        assertThrows(IllegalArgumentException.class, () -> db.addPendingVisit("nobody@vt.edu", chilis));
        assertThrows(IllegalArgumentException.class, () -> db.addPendingVisit(EMAIL, null));
        assertTrue(db.getPendingVisits("").isEmpty());
    }

    // ------------------------------------------------------------ ratings

    @Test
    public void ratingsComeBackNewestFirst() {
        db.saveProfile("jaidev", EMAIL);
        db.saveRating(EMAIL, chilis, 4, "Good chips");
        db.saveRating(EMAIL, subway, 2, "");
        ArrayList<Rating> ratings = db.getRatings(EMAIL);
        assertEquals(2, ratings.size());
        assertEquals("Subway", ratings.get(0).getRestaurantName());
        assertEquals(4, ratings.get(1).getStars());
        assertEquals("Good chips", ratings.get(1).getReview());
    }

    @Test
    public void badRatingsAreRejected() {
        db.saveProfile("jaidev", EMAIL);
        assertThrows(IllegalArgumentException.class, () -> db.saveRating(EMAIL, chilis, 0, ""));
        assertThrows(IllegalArgumentException.class, () -> db.saveRating(EMAIL, chilis, 6, ""));
        assertThrows(IllegalArgumentException.class, () -> db.saveRating("nobody@vt.edu", chilis, 3, ""));
        assertTrue(db.getRatings(EMAIL).isEmpty());
    }

    // ------------------------------------------------------------ last user

    @Test
    public void lastUserIsRememberedAndCleared() {
        assertNull(db.getLastUser());
        db.saveProfile("jaidev", EMAIL);
        db.setLastUser(EMAIL);
        db.close();
        db = new Database(dbPath);
        assertEquals(EMAIL, db.getLastUser());
        db.clearLastUser();
        assertNull(db.getLastUser());
    }

    @Test
    public void lastUserMustHaveAProfile() {
        assertThrows(IllegalArgumentException.class, () -> db.setLastUser("nobody@vt.edu"));
        assertThrows(IllegalArgumentException.class, () -> db.setLastUser(""));
    }

    // ------------------------------------------------------------ older database files

    @Test
    public void olderDatabaseFilesGetTheNewColumns() throws Exception {
        String oldPath = folder.resolve("old.db").toString();
        try (java.sql.Connection old = java.sql.DriverManager.getConnection("jdbc:sqlite:" + oldPath);
                java.sql.Statement statement = old.createStatement()) {
            statement.execute("CREATE TABLE profiles (email TEXT PRIMARY KEY COLLATE NOCASE, name TEXT NOT NULL)");
            statement.execute("INSERT INTO profiles VALUES ('old@vt.edu', 'Old User')");
        }
        try (Database upgraded = new Database(oldPath)) {
            Profile old = upgraded.loadProfile("old@vt.edu");
            assertEquals("Old User", old.getName());
            assertEquals(0, old.getAge());
        }
    }

    private static ArrayList<Tag> tags(String... names) {
        ArrayList<Tag> tags = new ArrayList<>();
        for (String name : names) {
            tags.add(new Tag(name));
        }
        return tags;
    }
}
