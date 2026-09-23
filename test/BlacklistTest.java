import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for Blacklist.
 */
public class BlacklistTest {
    private Blacklist blacklist;
    private Restaurant chipotle;
    private Restaurant sushiPlace;

    @BeforeEach
    public void setUp() {
        blacklist = new Blacklist();
        chipotle = new Restaurant("Chipotle", new ArrayList<>(Arrays.asList(new Tag("Mexican"))));
        sushiPlace = new Restaurant("Sushi Spot", new ArrayList<>(Arrays.asList(new Tag("Sushi"))));
    }

    @Test
    public void addRestaurantBlocksIt() {
        blacklist.addRestaurant(chipotle);
        assertTrue(blacklist.isBlocked(chipotle));
        assertFalse(blacklist.isBlocked(sushiPlace));
    }

    @Test
    public void addRestaurantSkipsDuplicates() {
        blacklist.addRestaurant(chipotle);
        blacklist.addRestaurant(chipotle);
        assertEquals(1, blacklist.getBlacklistedRestaurants().size());
    }

    @Test
    public void addNullRestaurantIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> blacklist.addRestaurant(null));
    }

    @Test
    public void removeRestaurantUnblocksIt() {
        blacklist.addRestaurant(chipotle);
        blacklist.removeRestaurant(chipotle);
        assertFalse(blacklist.isBlocked(chipotle));
        assertTrue(blacklist.getBlacklistedRestaurants().isEmpty());
    }

    @Test
    public void removeNullRestaurantIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> blacklist.removeRestaurant(null));
    }

    @Test
    public void getBlacklistedRestaurantsReturnsACopy() {
        blacklist.addRestaurant(chipotle);
        blacklist.getBlacklistedRestaurants().clear();
        assertEquals(Arrays.asList(chipotle), blacklist.getBlacklistedRestaurants());
    }

    @Test
    public void blacklistedTagBlocksEveryRestaurantWithIt() {
        blacklist.addTag(new Tag("sushi"));
        assertTrue(blacklist.isBlocked(sushiPlace));
        assertFalse(blacklist.isBlocked(chipotle));
        blacklist.removeTag(new Tag("Sushi"));
        assertFalse(blacklist.isBlocked(sushiPlace));
    }

    @Test
    public void nullTagsAndRestaurantsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> blacklist.addTag(null));
        assertThrows(IllegalArgumentException.class, () -> blacklist.removeTag(null));
        assertThrows(IllegalArgumentException.class, () -> blacklist.isBlocked(null));
    }
}
