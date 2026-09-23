import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for Restaurant.
 *
 * @author Jaidev Gogineni
 */
public class RestaurantTest {
    private static final double VT_LAT = 37.2296;
    private static final double VT_LNG = -80.4139;

    private Restaurant chilis;
    private Restaurant subway;

    @BeforeEach
    public void setUp() {
        chilis = new Restaurant("chili's", tags("American", "Mexican"));
        subway = new Restaurant("Subway", tags("Sandwiches"), 1, VT_LAT, VT_LNG, "100 Main St");
    }

    // ------------------------------------------------------------ constructor

    @Test
    public void constructorStoresNameAndTags() {
        assertEquals("chili's", chilis.getName());
        assertEquals(tags("American", "Mexican"), chilis.getTags());
    }

    @Test
    public void twoArgConstructorLeavesDetailsUnknown() {
        assertEquals(Restaurant.PRICE_UNKNOWN, chilis.getPriceLevel());
        assertFalse(chilis.hasLocation());
        assertEquals("", chilis.getAddress());
    }

    @Test
    public void fullConstructorStoresDetails() {
        assertEquals(1, subway.getPriceLevel());
        assertEquals(VT_LAT, subway.getLatitude());
        assertEquals(VT_LNG, subway.getLongitude());
        assertEquals("100 Main St", subway.getAddress());
    }

    @Test
    public void blankNameAndNullTagsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Restaurant("", null));
        assertThrows(IllegalArgumentException.class, () -> new Restaurant("Chipotle", null));
    }

    @Test
    public void nullTagInListIsRejected() {
        ArrayList<Tag> withNull = new ArrayList<>(Arrays.asList((Tag) null));
        assertThrows(IllegalArgumentException.class, () -> new Restaurant("Chipotle", withNull));
    }

    // ------------------------------------------------------------ tags

    @Test
    public void getTagsReturnsACopy() {
        chilis.getTags().add(new Tag("Italian"));
        assertEquals(2, chilis.getTags().size());
    }

    @Test
    public void addTagSkipsDuplicates() {
        chilis.addTag(new Tag("Bar"));
        chilis.addTag(new Tag("american"));
        assertEquals(3, chilis.getTags().size());
        assertTrue(chilis.hasTag(new Tag("bar")));
    }

    @Test
    public void addNullTagIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> chilis.addTag(null));
    }

    // ------------------------------------------------------------ distance

    @Test
    public void distanceIsNaNUntilSet() {
        assertTrue(Double.isNaN(chilis.getDistance()));
        chilis.setDistance(2.5);
        assertEquals(2.5, chilis.getDistance());
    }

    @Test
    public void badDistancesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> chilis.setDistance(-5));
        assertThrows(IllegalArgumentException.class, () -> chilis.setDistance(Double.NaN));
    }

    // ------------------------------------------------------------ price

    @Test
    public void priceSymbolMatchesLevel() {
        subway.setPriceLevel(3);
        assertEquals("$$$", subway.getPriceSymbol());
        assertEquals("?", chilis.getPriceSymbol());
    }

    @Test
    public void priceOutsideZeroToFourIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> subway.setPriceLevel(5));
        assertThrows(IllegalArgumentException.class, () -> subway.setPriceLevel(-1));
    }

    // ------------------------------------------------------------ location

    @Test
    public void setLocationStoresCoordinates() {
        chilis.setLocation(37.14, -80.405);
        assertTrue(chilis.hasLocation());
        assertEquals(37.14, chilis.getLatitude());
    }

    @Test
    public void badLocationsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> subway.setLocation(91, 0));
        assertThrows(IllegalArgumentException.class, () -> subway.setLocation(VT_LAT, Double.NaN));
    }

    // ------------------------------------------------------------ reviews and menu

    @Test
    public void reviewsAndMenuItemsAreStored() {
        subway.addReview("Fast and cheap");
        subway.addMenuItem("Italian BMT");
        assertEquals(Arrays.asList("Fast and cheap"), subway.getReviews());
        assertEquals(Arrays.asList("Italian BMT"), subway.getMenu());
    }

    @Test
    public void blankReviewsAndMenuItemsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> subway.addReview(" "));
        assertThrows(IllegalArgumentException.class, () -> subway.addMenuItem(null));
    }

    // ------------------------------------------------------------ equals

    @Test
    public void sameNameAndAddressAreEqual() {
        assertEquals(subway, new Restaurant("SUBWAY", tags(), 2, 0, 0, "100 main st"));
        assertNotEquals(subway, new Restaurant("Subway", tags(), 1, VT_LAT, VT_LNG, "200 Main St"));
    }

    private static ArrayList<Tag> tags(String... names) {
        ArrayList<Tag> tags = new ArrayList<>();
        for (String name : names) {
            tags.add(new Tag(name));
        }
        return tags;
    }
}
