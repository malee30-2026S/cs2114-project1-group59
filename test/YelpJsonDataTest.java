import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import org.junit.jupiter.api.Test;

public class YelpJsonDataTest {
    @Test
    public void parseYelpJsonExtractsBlacksburgBusinesses() {
        Data data = new Data();
        String json = "{\n"
                + "  \"businesses\": [\n"
                + "    {\n"
                + "      \"name\": \"The Cellar\",\n"
                + "      \"categories\": [\n"
                + "        {\"title\": \"Italian\"},\n"
                + "        {\"title\": \"Pizza\"}\n"
                + "      ],\n"
                + "      \"location\": {\n"
                + "        \"city\": \"Blacksburg\",\n"
                + "        \"display_address\": [\"123 Main St\", \"Blacksburg, VA\"]\n"
                + "      },\n"
                + "      \"coordinates\": {\"latitude\": 37.2296, \"longitude\": -80.4139},\n"
                + "      \"price\": \"$$\",\n"
                + "      \"rating\": 4.7,\n"
                + "      \"review_count\": 121\n"
                + "    },\n"
                + "    {\n"
                + "      \"name\": \"Away Place\",\n"
                + "      \"categories\": [{\"title\": \"Mexican\"}],\n"
                + "      \"location\": {\"city\": \"Roanoke\"}\n"
                + "    }\n"
                + "  ]\n"
                + "}";

        ArrayList<Restaurant> businesses = data.parseYelpJson(json);

        assertEquals(1, businesses.size());
        assertEquals("The Cellar", businesses.get(0).getName());
        assertTrue(businesses.get(0).hasTag(new Tag("Italian")));
        assertTrue(businesses.get(0).hasTag(new Tag("Pizza")));
        assertEquals(2, businesses.get(0).getPriceLevel());
        assertTrue(businesses.get(0).hasLocation());
    }

    @Test
    public void badJsonGivesNoRestaurantsInsteadOfCrashing() {
        Data data = new Data();
        assertTrue(data.parseYelpJson(null).isEmpty());
        assertTrue(data.parseYelpJson("").isEmpty());
        assertTrue(data.parseYelpJson("not json at all").isEmpty());
        assertTrue(data.parseYelpJson("{\"businesses\": [").isEmpty());
    }
}
