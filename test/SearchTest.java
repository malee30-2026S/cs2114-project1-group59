import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for Search.
 */
public class SearchTest {
    private Restaurant chilis;
    private Restaurant subway;
    private Restaurant chipotle;
    private Restaurant mystery;
    private Search search;

    @BeforeEach
    public void setUp() {
        chilis = new Restaurant("Chili's", tags("American", "Mexican"), 2, 37.14, -80.405, "");
        subway = new Restaurant("Subway", tags("Sandwiches"), 1, 37.23, -80.414, "");
        chipotle = new Restaurant("Chipotle", tags("Mexican"), 1, 37.231, -80.425, "");
        mystery = new Restaurant("Mystery Diner", tags("American"));
        chilis.setDistance(6.2);
        subway.setDistance(0.1);
        chipotle.setDistance(0.6);
        search = new Search(new ArrayList<>(Arrays.asList(chilis, subway, chipotle, mystery)));
    }

    @Test
    public void nullListIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Search(null));
    }

    // ------------------------------------------------------------ searchByName

    @Test
    public void searchByNameFindsPartialMatchesIgnoringCase() {
        assertEquals(Arrays.asList(chilis, chipotle), search.searchByName("CHI"));
        assertTrue(search.searchByName("pizza").isEmpty());
    }

    @Test
    public void searchByBlankNameIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> search.searchByName(""));
        assertThrows(IllegalArgumentException.class, () -> search.searchByName(null));
    }

    // ------------------------------------------------------------ searchByTag

    @Test
    public void searchByTagFindsRestaurantsWithIt() {
        assertEquals(Arrays.asList(chilis, chipotle), search.searchByTag(new Tag("mexican")));
    }

    @Test
    public void searchByNullTagIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> search.searchByTag(null));
    }

    // ------------------------------------------------------------ searchByPrice

    @Test
    public void searchByPriceIsCheapestFirstAndSkipsUnknownPrices() {
        assertEquals(Arrays.asList(chipotle, subway), search.searchByPrice(1));
        assertEquals(Arrays.asList(chipotle, subway, chilis), search.searchByPrice(4));
    }

    @Test
    public void searchByBadPriceIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> search.searchByPrice(-1));
        assertThrows(IllegalArgumentException.class, () -> search.searchByPrice(0));
        assertThrows(IllegalArgumentException.class, () -> search.searchByPrice(Integer.MAX_VALUE));
        assertThrows(IllegalArgumentException.class, () -> search.searchByPrice(Integer.MIN_VALUE));
    }

    // ------------------------------------------------------------ searchByDistance

    @Test
    public void searchByDistanceIsClosestFirstAndSkipsUnknownDistances() {
        assertEquals(Arrays.asList(subway, chipotle), search.searchByDistance(1));
        assertEquals(Arrays.asList(subway, chipotle, chilis), search.searchByDistance(10));
    }

    @Test
    public void searchByBadDistanceIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> search.searchByDistance(-5));
        assertThrows(IllegalArgumentException.class, () -> search.searchByDistance(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> search.searchByDistance(Integer.MAX_VALUE));
    }

    // ------------------------------------------------------------ searchByDish

    @Test
    public void searchByDishFindsCuisinesThatServeIt() {
        assertEquals(Arrays.asList(chilis, chipotle), search.searchByDish("Tacos"));
        assertEquals(Arrays.asList(subway), search.searchByDish("turkey sub"));
    }

    @Test
    public void menuMatchesComeFirst() {
        mystery.addMenuItem("Breakfast Tacos");
        assertEquals(Arrays.asList(mystery, chilis, chipotle), search.searchByDish("tacos"));
    }

    @Test
    public void unknownDishFindsNothing() {
        assertTrue(search.searchByDish("zzzz").isEmpty());
        assertTrue(Search.cuisinesForDish("zzzz").isEmpty());
    }

    @Test
    public void searchByBlankDishIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> search.searchByDish(" "));
        assertThrows(IllegalArgumentException.class, () -> search.searchByDish(null));
    }

    @Test
    public void cuisinesForDishKnowsCommonDishes() {
        assertEquals(Arrays.asList("Thai"), Search.cuisinesForDish("Pad Thai"));
        assertEquals(Arrays.asList("Pizza", "Italian"), Search.cuisinesForDish("pepperoni pizza"));
        assertEquals(Arrays.asList("Mexican", "Seafood"), Search.cuisinesForDish("fish tacos"));
    }

    // ------------------------------------------------------------ blacklist

    @Test
    public void blacklistedRestaurantsNeverShowUp() {
        Blacklist blacklist = new Blacklist();
        blacklist.addRestaurant(chipotle);
        search.setBlacklist(blacklist);
        assertEquals(Arrays.asList(chilis), search.searchByTag(new Tag("Mexican")));
        assertEquals(Arrays.asList(chilis), search.searchByName("chi"));
    }

    private static ArrayList<Tag> tags(String... names) {
        ArrayList<Tag> tags = new ArrayList<>();
        for (String name : names) {
            tags.add(new Tag(name));
        }
        return tags;
    }
}
