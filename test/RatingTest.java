import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Tests for Rating.
 *
 * @author Jaidev Gogineni
 */
public class RatingTest {
    @Test
    public void storesEverything() {
        Rating rating = new Rating(" Chipotle ", "314 N Main St", 5, " Great ", "2026-09-23 18:30:00");
        assertEquals("Chipotle", rating.getRestaurantName());
        assertEquals("314 N Main St", rating.getRestaurantAddress());
        assertEquals(5, rating.getStars());
        assertEquals("Great", rating.getReview());
        assertEquals("2026-09-23", rating.getDate());
    }

    @Test
    public void missingReviewAndAddressBecomeEmpty() {
        Rating rating = new Rating("Chipotle", null, 3, null, null);
        assertEquals("", rating.getRestaurantAddress());
        assertEquals("", rating.getReview());
        assertEquals("", rating.getDate());
    }

    @Test
    public void badRatingsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Rating("Chipotle", "", 0, "", ""));
        assertThrows(IllegalArgumentException.class, () -> new Rating("Chipotle", "", 6, "", ""));
        assertThrows(IllegalArgumentException.class, () -> new Rating(" ", "", 3, "", ""));
    }
}
