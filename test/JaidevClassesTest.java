import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Tests for Tag, Restaurant, DistanceCalculator, and Database, following
 * the good-input / bad-input rows of the Project #1 test plan.
 *
 * No test framework needed. Run it and every line should say PASS; the
 * program exits with code 1 if anything fails.
 *
 * @author Jaidev Gogineni
 */
public class JaidevClassesTest {
    private static int passed = 0;
    private static int failed = 0;

    // Virginia Tech campus, from the test plan
    private static final double VT_LAT = 37.2296;
    private static final double VT_LNG = -80.4139;

    public static void main(String[] args) throws Exception {
        testTag();
        testRestaurant();
        testDistanceCalculator();
        testDatabase();
        System.out.println();
        System.out.println(passed + " passed, " + failed + " failed");
        System.exit(failed == 0 ? 0 : 1);
    }

    private static void testTag() {
        section("Tag");
        Tag mexican = new Tag("Mexican");
        check("Tag(\"Mexican\").getName() is Mexican", mexican.getName().equals("Mexican"));
        check("tags match ignoring case", mexican.equals(new Tag("  mexican ")));
        check("equal tags have equal hash codes", mexican.hashCode() == new Tag("MEXICAN").hashCode());
        checkThrows("Tag(null) is rejected", () -> new Tag(null));
        checkThrows("Tag(\"\") is rejected", () -> new Tag("   "));
    }

    private static void testRestaurant() {
        section("Restaurant");
        Restaurant chilis = new Restaurant("chili's", tags("American", "Mexican"));
        check("Restaurant(\"chili's\", [american, mexican]) creates it", chilis.getName().equals("chili's"));
        check("getTags() returns both tags", chilis.getTags().equals(tags("American", "Mexican")));
        check("hasTag(mexican) is true", chilis.hasTag(new Tag("mexican")));
        check("getDistance() is NaN before it's calculated", Double.isNaN(chilis.getDistance()));
        check("price is unknown by default", chilis.getPriceLevel() == Restaurant.PRICE_UNKNOWN);
        check("location is unknown by default", !chilis.hasLocation());

        chilis.getTags().add(new Tag("Italian"));
        check("editing the list from getTags() doesn't change the restaurant", chilis.getTags().size() == 2);
        chilis.addTag(new Tag("american"));
        check("adding a duplicate tag does nothing", chilis.getTags().size() == 2);

        Restaurant full = new Restaurant("Subway", tags("Sandwiches"), 1, VT_LAT, VT_LNG, "100 Main St");
        check("full constructor stores price", full.getPriceLevel() == 1 && full.getPriceSymbol().equals("$"));
        check("full constructor stores location", full.hasLocation() && full.getLatitude() == VT_LAT);
        check("same name + address are equal", full.equals(new Restaurant("SUBWAY", tags(), 2, 0, 0, "100 main st")));
        check("same name, different address are not equal",
                !full.equals(new Restaurant("Subway", tags(), 1, VT_LAT, VT_LNG, "200 Main St")));

        full.addReview("Fast and cheap");
        full.addMenuItem("Italian BMT");
        check("addReview / addMenuItem store text", full.getReviews().size() == 1 && full.getMenu().size() == 1);

        checkThrows("Restaurant(\"\", null) is rejected", () -> new Restaurant("", null));
        checkThrows("null tag list is rejected", () -> new Restaurant("Chipotle", null));
        checkThrows("null tag inside list is rejected",
                () -> new Restaurant("Chipotle", new ArrayList<>(Arrays.asList((Tag) null))));
        checkThrows("price level 5 is rejected", () -> full.setPriceLevel(5));
        checkThrows("price level -1 is rejected", () -> full.setPriceLevel(-1));
        checkThrows("latitude 91 is rejected", () -> full.setLocation(91, 0));
        checkThrows("only one coordinate set is rejected", () -> full.setLocation(VT_LAT, Double.NaN));
        checkThrows("negative distance is rejected", () -> full.setDistance(-5));
        checkThrows("NaN distance is rejected", () -> full.setDistance(Double.NaN));
        checkThrows("blank review is rejected", () -> full.addReview(" "));
    }

    private static void testDistanceCalculator() {
        section("DistanceCalculator");
        DistanceCalculator calc = new DistanceCalculator(null);
        check("no API key means straight-line mode", !calc.isUsingGoogleMaps());

        double nycToLa = DistanceCalculator.straightLineDistance(40.7128, -74.0060, 34.0522, -118.2437);
        check("New York to Los Angeles is about 2445 miles (got " + round(nycToLa) + ")",
                Math.abs(nycToLa - 2445) < 5);
        check("same point is 0 miles", calc.getDistance(VT_LAT, VT_LNG, VT_LAT, VT_LNG) == 0);

        Restaurant nearby = new Restaurant("Chipotle", tags("Mexican"), 1, 37.2310, -80.4250, "");
        double miles = calc.getDistance(VT_LAT, VT_LNG, nearby);
        check("getDistance(user, restaurant) stores it on the restaurant", nearby.getDistance() == miles);
        check("a restaurant ~0.6 mi away measures 0.5-0.8 mi (got " + round(miles) + ")",
                miles > 0.5 && miles < 0.8);

        Restaurant noLocation = new Restaurant("Mystery Diner", tags());
        ArrayList<Restaurant> list = new ArrayList<>(Arrays.asList(nearby, noLocation, null));
        calc.updateDistances(VT_LAT, VT_LNG, list);
        check("updateDistances skips restaurants with no location", Double.isNaN(noLocation.getDistance()));

        double[] parsed = DistanceCalculator.parseCoordinates("37.2296\u00B0 N, 80.4139\u00B0 W");
        check("parses \"37.2296\u00B0 N, 80.4139\u00B0 W\"", parsed[0] == VT_LAT && parsed[1] == VT_LNG);
        parsed = DistanceCalculator.parseCoordinates("37.2296, -80.4139");
        check("parses \"37.2296, -80.4139\"", parsed[0] == VT_LAT && parsed[1] == VT_LNG);

        checkThrows("parseCoordinates(\"\") is rejected", () -> DistanceCalculator.parseCoordinates(""));
        checkThrows("parseCoordinates(\"Blacksburg\") is rejected",
                () -> DistanceCalculator.parseCoordinates("Blacksburg"));
        checkThrows("latitude 200 is rejected", () -> calc.getDistance(200, 0, 0, 0));
        checkThrows("NaN longitude is rejected", () -> calc.getDistance(0, Double.NaN, 0, 0));
        checkThrows("restaurant with no location is rejected", () -> calc.getDistance(VT_LAT, VT_LNG, noLocation));
        checkThrows("null restaurant list is rejected", () -> calc.updateDistances(VT_LAT, VT_LNG, null));
        checkThrows("null travel mode is rejected", () -> calc.setTravelMode(null));
    }

    private static void testDatabase() throws Exception {
        section("Database");
        File file = Files.createTempFile("hungryhokie-test", ".db").toFile();
        try {
            try (Database db = new Database(file.getPath())) {
                Restaurant chilis = new Restaurant("Chili's", tags("American", "Mexican"), 2,
                        37.1400, -80.4050, "1 Test Rd");
                chilis.addReview("Good chips");
                chilis.addMenuItem("Fajitas");
                db.saveRestaurant(chilis);
                db.saveRestaurant(new Restaurant("Subway", tags("Sandwiches"), 1, VT_LAT, VT_LNG, "2 Test Rd"));

                Restaurant found = db.findRestaurant("chili's");
                check("findRestaurant(\"chili's\") finds it, ignoring case", found != null);
                check("saved tags come back", found.getTags().size() == 2 && found.hasTag(new Tag("mexican")));
                check("saved price and location come back",
                        found.getPriceLevel() == 2 && found.getLatitude() == 37.1400);
                check("saved review and menu come back",
                        found.getReviews().equals(list("Good chips")) && found.getMenu().equals(list("Fajitas")));
                check("findRestaurant(\"none\") returns null", db.findRestaurant("none") == null);
                check("findRestaurant(\"\") returns null", db.findRestaurant("") == null);

                String injection = "x'; DROP TABLE restaurants; --";
                check("SQL injection returns null", db.findRestaurant(injection) == null);
                check("...and the table is still there", db.getAllRestaurants().size() == 2);

                chilis.addTag(new Tag("Bar"));
                db.saveRestaurant(chilis);
                check("saving again updates instead of duplicating", db.getAllRestaurants().size() == 2);
                check("...and the new tag is saved", db.findRestaurant("Chili's").hasTag(new Tag("bar")));

                check("getRestaurantsByTag(mexican) finds Chili's only",
                        db.getRestaurantsByTag(new Tag("MEXICAN")).equals(list(chilis)));
                check("getAllTags() lists every tag", db.getAllTags().size() == 4);
                check("removeRestaurant returns true", db.removeRestaurant(chilis));
                check("...and it's gone", db.findRestaurant("Chili's") == null);
                check("removing it again returns false", !db.removeRestaurant(chilis));

                db.saveProfile("jaidev", "jaidevg@vt.edu");
                check("saveProfile(jaidev, jaidevg@vt.edu) saves it", db.profileExists("JAIDEVG@vt.edu"));
                check("getProfileName returns the name", "jaidev".equals(db.getProfileName("jaidevg@vt.edu")));
                db.adjustTagWeight("jaidevg@vt.edu", new Tag("Italian"), 2.0);
                db.adjustTagWeight("jaidevg@vt.edu", new Tag("italian"), 1.5);
                db.adjustTagWeight("jaidevg@vt.edu", new Tag("Sushi"), -1.0);
                HashMap<Tag, Double> weights = db.getTagWeights("jaidevg@vt.edu");
                check("tag weights add up (Italian = 3.5)", Double.valueOf(3.5).equals(weights.get(new Tag("Italian"))));
                check("negative weights work (Sushi = -1.0)", Double.valueOf(-1.0).equals(weights.get(new Tag("Sushi"))));
                check("unknown profile has no weights", db.getTagWeights("nobody@vt.edu").isEmpty());

                checkThrows("saveRestaurant(null) is rejected", () -> db.saveRestaurant(null));
                checkThrows("saveProfile( , ) is rejected", () -> db.saveProfile("", ""));
                checkThrows("weight for a missing profile is rejected",
                        () -> db.adjustTagWeight("nobody@vt.edu", new Tag("Italian"), 1));
                checkThrows("NaN weight change is rejected",
                        () -> db.adjustTagWeight("jaidevg@vt.edu", new Tag("Italian"), Double.NaN));
                checkThrows("getRestaurantsByTag(null) is rejected", () -> db.getRestaurantsByTag(null));
            }

            try (Database reopened = new Database(file.getPath())) {
                check("data is still there after closing and reopening",
                        reopened.findRestaurant("Subway") != null
                        && Double.valueOf(3.5).equals(reopened.getTagWeights("jaidevg@vt.edu").get(new Tag("Italian"))));
            }
            checkThrows("blank database path is rejected", () -> new Database(" "));
        }
        finally {
            file.delete();
        }
    }

    // ---------------------------------------------------------------- Helpers

    private static ArrayList<Tag> tags(String... names) {
        ArrayList<Tag> tags = new ArrayList<>();
        for (String name : names) {
            tags.add(new Tag(name));
        }
        return tags;
    }

    @SafeVarargs
    private static <T> ArrayList<T> list(T... items) {
        return new ArrayList<>(Arrays.asList(items));
    }

    private static String round(double value) {
        return String.format("%.2f", value);
    }

    private static void section(String name) {
        System.out.println();
        System.out.println("== " + name + " ==");
    }

    private static void check(String description, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("PASS  " + description);
        }
        else {
            failed++;
            System.out.println("FAIL  " + description);
        }
    }

    private static void checkThrows(String description, Runnable code) {
        try {
            code.run();
            failed++;
            System.out.println("FAIL  " + description + " (no exception thrown)");
        }
        catch (IllegalArgumentException e) {
            passed++;
            System.out.println("PASS  " + description);
        }
        catch (RuntimeException e) {
            failed++;
            System.out.println("FAIL  " + description + " (threw " + e + ")");
        }
    }
}
