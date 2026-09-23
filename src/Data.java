import java.util.ArrayList;
import java.util.HashMap;

/**
 * Loads a Yelp-like dataset and turns it into searchable Restaurant objects,
 * while preserving additional metadata such as hours and bios.
 */
public class Data {
    private final ArrayList<Restaurant> restaurants;
    private final ArrayList<Tag> tags;
    private final HashMap<Restaurant, String> hoursByRestaurant;
    private final HashMap<Restaurant, String> bioByRestaurant;

    public Data() {
        restaurants = new ArrayList<>();
        tags = new ArrayList<>();
        hoursByRestaurant = new HashMap<>();
        bioByRestaurant = new HashMap<>();
    }

    /**
     * Adds a tag to the global tag list if it is not already present.
     *
     * @param tagName the tag to add, e.g. "Italian" or "Umami"
     * @return the tag object
     */
    public Tag createTag(String tagName) {
        if (tagName == null || tagName.isBlank()) {
            throw new IllegalArgumentException("Tag name cannot be empty");
        }
        Tag tag = new Tag(tagName.trim());
        if (!tags.contains(tag)) {
            tags.add(tag);
        }
        return tag;
    }

    /**
     * Adds a restaurant to the data set, along with metadata relevant to the
     * app.
     */
    public void addRestaurant(Restaurant restaurant, String hoursOpen, String bio) {
        if (restaurant == null) {
            throw new IllegalArgumentException("Restaurant cannot be null");
        }
        if (!restaurants.contains(restaurant)) {
            restaurants.add(restaurant);
        }
        for (Tag tag : restaurant.getTags()) {
            if (!tags.contains(tag)) {
                tags.add(tag);
            }
        }
        hoursByRestaurant.put(restaurant, hoursOpen == null ? "" : hoursOpen.trim());
        bioByRestaurant.put(restaurant, bio == null ? "" : bio.trim());
    }

    public void addRestaurant(Restaurant restaurant) {
        addRestaurant(restaurant, "", "");
    }

    public ArrayList<Restaurant> getRestaurants() {
        return new ArrayList<>(restaurants);
    }

    public ArrayList<Tag> getTags() {
        return new ArrayList<>(tags);
    }

    public String getHoursOpen(Restaurant restaurant) {
        if (restaurant == null) {
            throw new IllegalArgumentException("Restaurant cannot be null");
        }
        return hoursByRestaurant.getOrDefault(restaurant, "");
    }

    public String getBio(Restaurant restaurant) {
        if (restaurant == null) {
            throw new IllegalArgumentException("Restaurant cannot be null");
        }
        return bioByRestaurant.getOrDefault(restaurant, "");
    }

    /**
     * Creates a sample dataset for the Blacksburg/Christiansburg area so the
     * app can show real restaurant objects without needing an external API
     * at startup.
     *
     * Names, cuisines, addresses, and coordinates come from OpenStreetMap.
     * Price levels are estimates (fast food = $, sit-down = $$). The last
     * four of the original picks weren't found on the map, so their price
     * and distance are unknown.
     */
    public static Data loadYelpAreaData() {
        final double unknown = Double.NaN;
        Data data = new Data();
        // name, tags, price, latitude, longitude, address, hours, bio
        data.addSample("The Cellar", "Italian", 2, 37.23089, -80.41510, "302 N Main St, Blacksburg",
                "11:00 AM - 9:00 PM", "Cozy college favorite with pasta and pizza.");
        data.addSample("Mellow Mushroom", "Pizza", 2, 37.22822, -80.41242, "207 S Main St, Blacksburg",
                "11:00 AM - 10:00 PM", "Famous for stone-baked pizza and laid-back vibes.");
        data.addSample("Taco Bell", "Mexican", 1, 37.23377, -80.41845, "608 N Main St, Blacksburg",
                "9:00 AM - 1:00 AM", "Fast, affordable, and open late.");
        data.addSample("Cabo Fish Taco", "Seafood, Mexican", 2, 37.22884, -80.41315, "117 S Main St, Blacksburg",
                "11:00 AM - 9:00 PM", "Tacos, rice bowls, and a lively local favorite.");
        data.addSample("Sushi Garden", "Japanese", 0, unknown, unknown, "",
                "12:00 PM - 9:00 PM", "Fresh rolls, rice bowls, and quick service.");
        data.addSample("Cafe 13", "American", 0, unknown, unknown, "",
                "8:00 AM - 10:00 PM", "Popular breakfast and burger spot near campus.");
        data.addSample("Burrito Union", "Mexican", 0, unknown, unknown, "",
                "10:00 AM - 9:00 PM", "Build-your-own burritos and casual Tex-Mex.");
        data.addSample("Pita Bowl", "Mediterranean", 0, unknown, unknown, "",
                "11:00 AM - 8:00 PM", "Healthy bowls, wraps, and salads.");

        data.addSample("Chipotle", "Mexican", 1, 37.23106, -80.41525, "314 N Main St, Blacksburg", "", "");
        data.addSample("Zeppoli's Italian Restaurant", "Italian", 2, 37.23393, -80.43176,
                "810 University City Blvd, Blacksburg", "", "");
        data.addSample("Not Your Mama's Pasta", "Italian", 2, 37.22759, -80.41191, "301 S Main St, Blacksburg", "", "");
        data.addSample("Sushi Factory", "Japanese, Sushi", 2, 37.23394, -80.43442,
                "801 University City Blvd, Blacksburg", "", "");
        data.addSample("Hefun", "Japanese, Sushi, Ramen", 2, 37.23207, -80.41600, "428 N Main St, Blacksburg", "", "");
        data.addSample("Mezeh Mediterranean Grill", "Mediterranean", 1, 37.23258, -80.43241,
                "616 University City Blvd, Blacksburg", "", "");
        data.addSample("Souvlaki", "Mediterranean, Greek", 1, 37.22990, -80.41627, "201 College Ave, Blacksburg", "", "");
        data.addSample("Happy Wok", "Chinese", 1, 37.23026, -80.41575, "141 College Ave, Blacksburg", "", "");
        data.addSample("Junzi", "Chinese", 1, 37.23359, -80.42183, "220 Gilbert St, Blacksburg", "", "");
        data.addSample("India Garden", "Indian", 2, 37.23473, -80.42177, "210 Prices Fork Rd, Blacksburg", "", "");
        data.addSample("Namaste Kitchen", "Indian, Nepalese", 2, 37.23051, -80.41536, "239 N Main St, Blacksburg", "", "");
        data.addSample("Yeah Siam", "Thai", 2, 37.23028, -80.41415, "104 Jackson St, Blacksburg", "", "");
        data.addSample("Lefty's", "American, Burgers", 2, 37.21520, -80.40106, "1410 S Main St, Blacksburg", "", "");
        data.addSample("Cook Out", "American, Burgers", 1, 37.21704, -80.40074, "1311 S Main St, Blacksburg", "", "");
        data.addSample("Subway", "Sandwiches", 1, 37.23512, -80.43329, "860 University City Blvd, Blacksburg", "", "");
        data.addSample("Mission BBQ", "BBQ", 2, 37.16468, -80.42058, "2585 Market St NE, Christiansburg", "", "");
        data.addSample("Sandro's Pizzeria Italian Restaurant", "Pizza, Italian", 2, 37.16783, -80.42065,
                "2775 Market St NE, Christiansburg", "", "");
        data.addSample("Gran Rodeo Mexican Restaurant", "Mexican", 2, 37.16280, -80.41924,
                "200 Laurel St, Christiansburg", "", "");
        data.addSample("Panda Express", "Chinese", 1, 37.16298, -80.42796,
                "250 Peppers Ferry Rd NW, Christiansburg", "", "");
        data.addSample("Five Guys", "American, Burgers", 1, 37.15843, -80.42117,
                "95 Spradlin Farm Dr, Christiansburg", "", "");
        return data;
    }

    /**
     * Builds one sample restaurant and adds it.
     *
     * @param tagList comma-separated tags, e.g. "Pizza, Italian"
     */
    private void addSample(String name, String tagList, int price, double latitude, double longitude,
            String address, String hoursOpen, String bio) {
        ArrayList<Tag> sampleTags = new ArrayList<>();
        for (String tagName : tagList.split(",")) {
            sampleTags.add(createTag(tagName));
        }
        addRestaurant(new Restaurant(name, sampleTags, price, latitude, longitude, address), hoursOpen, bio);
    }

    public static ArrayList<Restaurant> buildYelpAreaDataset() {
        return loadYelpAreaData().getRestaurants();
    }

    /**
     * Parses a Yelp-like row and creates Restaurant objects from it.
     *
     * Expected row format: [name, tag1, tag2, ..., hoursOpen, bio]
     */
    public ArrayList<Restaurant> parseYelpData(ArrayList<String[]> rows) {
        restaurants.clear();
        tags.clear();
        hoursByRestaurant.clear();
        bioByRestaurant.clear();

        ArrayList<Restaurant> parsed = new ArrayList<>();
        if (rows == null) {
            return parsed;
        }

        for (String[] row : rows) {
            if (row == null || row.length == 0 || row[0] == null || row[0].isBlank()) {
                continue;
            }

            String name = row[0].trim();
            ArrayList<Tag> rowTags = new ArrayList<>();
            String hoursOpen = "";
            String bio = "";

            int tagStart = 1;
            if (row.length >= 3) {
                hoursOpen = row[row.length - 2] == null ? "" : row[row.length - 2].trim();
                bio = row[row.length - 1] == null ? "" : row[row.length - 1].trim();
                tagStart = 1;
            }

            for (int i = tagStart; i < row.length; i++) {
                if (i == row.length - 2 && row.length >= 3) {
                    break;
                }
                if (row[i] == null || row[i].isBlank()) {
                    continue;
                }
                Tag tag = createTag(row[i]);
                rowTags.add(tag);
            }

            Restaurant restaurant = new Restaurant(name, rowTags);
            addRestaurant(restaurant, hoursOpen, bio);
            parsed.add(restaurant);
        }

        return parsed;
    }

    /**
     * Convenience method for a dataset stored as a list of string lists.
     */
    public ArrayList<Restaurant> parseYelpDataList(ArrayList<ArrayList<String>> rows) {
        if (rows == null) {
            return new ArrayList<>();
        }
        ArrayList<String[]> normalized = new ArrayList<>();
        for (ArrayList<String> row : rows) {
            if (row != null) {
                normalized.add(row.toArray(new String[0]));
            }
        }
        return parseYelpData(normalized);
    }

    /**
     * Parses raw Yelp JSON, keeps only businesses in the Blacksburg/
     * Christiansburg area, and turns each entry into a Restaurant object.
     */
    public ArrayList<Restaurant> parseYelpJson(String json) {
        restaurants.clear();
        tags.clear();
        hoursByRestaurant.clear();
        bioByRestaurant.clear();

        ArrayList<Restaurant> parsed = new ArrayList<>();
        if (json == null || json.isBlank()) {
            return parsed;
        }

        String businessesText = extractArrayValue(json, "businesses");
        if (businessesText == null || businessesText.isBlank()) {
            return parsed;
        }

        for (String businessText : splitJsonObjects(businessesText)) {
            if (businessText == null || businessText.isBlank()) {
                continue;
            }

            String city = readStringField(extractObjectValue(businessText, "location"), "city");
            if (!isInArea(city)) {
                continue;
            }

            String name = readStringField(businessText, "name");
            if (name == null || name.isBlank()) {
                continue;
            }

            ArrayList<Tag> allTags = new ArrayList<>();
            String categoriesText = extractArrayValue(businessText, "categories");
            if (categoriesText != null && !categoriesText.isBlank()) {
                for (String categoryObject : splitJsonObjects(categoriesText)) {
                    String categoryTitle = readStringField(categoryObject, "title");
                    if (categoryTitle != null && !categoryTitle.isBlank()) {
                        allTags.add(createTag(categoryTitle));
                    }
                }
            }

            if (allTags.isEmpty()) {
                String alias = readStringField(businessText, "alias");
                if (alias != null && !alias.isBlank()) {
                    allTags.add(createTag(alias.replace('-', ' ').trim()));
                }
            }

            Restaurant restaurant = new Restaurant(name, allTags);

            String price = readStringField(businessText, "price");
            if (price != null && !price.isBlank()) {
                restaurant.setPriceLevel(Math.max(1, price.length()));
            }

            String address = readAddress(businessText);
            if (address != null && !address.isBlank()) {
                restaurant.setAddress(address);
            }

            double latitude = readDoubleField(extractObjectValue(businessText, "coordinates"), "latitude");
            double longitude = readDoubleField(extractObjectValue(businessText, "coordinates"), "longitude");
            if (!Double.isNaN(latitude) && !Double.isNaN(longitude)) {
                restaurant.setLocation(latitude, longitude);
            }

            String hoursOpen = readHoursText(businessText);
            String bio = buildBio(businessText);
            addRestaurant(restaurant, hoursOpen, bio);
            parsed.add(restaurant);
        }

        return parsed;
    }

    private static boolean isInArea(String city) {
        if (city == null) {
            return false;
        }
        String normalized = city.trim();
        return "Blacksburg".equalsIgnoreCase(normalized)
                || "Christiansburg".equalsIgnoreCase(normalized)
                || "Blacksburg/Christiansburg".equalsIgnoreCase(normalized)
                || "Blacksburg, VA".equalsIgnoreCase(normalized)
                || "Christiansburg, VA".equalsIgnoreCase(normalized);
    }

    private static String readAddress(String businessText) {
        String locationText = extractObjectValue(businessText, "location");
        if (locationText == null || locationText.isBlank()) {
            return "";
        }
        String display = extractArrayValue(locationText, "display_address");
        if (display == null || display.isBlank()) {
            return "";
        }
        ArrayList<String> items = readStringArrayItems(display);
        if (items.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(items.get(i));
        }
        return builder.toString();
    }

    private static String buildBio(String businessText) {
        String rating = readStringField(businessText, "rating");
        String count = readStringField(businessText, "review_count");
        String city = readStringField(extractObjectValue(businessText, "location"), "city");
        if (rating == null && count == null && city == null) {
            return "Yelp listing";
        }
        StringBuilder bio = new StringBuilder();
        if (rating != null && !rating.isBlank()) {
            bio.append("Rating: ").append(rating).append(" ");
        }
        if (count != null && !count.isBlank()) {
            bio.append("(" ).append(count).append(" reviews)");
        }
        if (city != null && !city.isBlank()) {
            if (bio.length() > 0) {
                bio.append(" - ");
            }
            bio.append("Serves ").append(city);
        }
        return bio.length() == 0 ? "Yelp listing" : bio.toString();
    }

    private static String readHoursText(String businessText) {
        String isOpenText = readStringField(businessText, "is_open_now");
        if (isOpenText == null || isOpenText.isBlank()) {
            return "Hours unavailable";
        }
        return "true".equalsIgnoreCase(isOpenText.trim()) ? "Open now" : "Closed now";
    }

    private static String extractArrayValue(String json, String key) {
        String value = extractObjectValue(json, key);
        if (value == null || value.isBlank()) {
            return null;
        }
        if (value.startsWith("[") && value.endsWith("]")) {
            return value;
        }
        return null;
    }

    private static String extractObjectValue(String json, String key) {
        if (json == null || json.isBlank()) {
            return null;
        }
        String needle = "\"" + key + "\"";
        int keyIndex = json.indexOf(needle);
        while (keyIndex >= 0) {
            int colonIndex = json.indexOf(':', keyIndex + needle.length());
            if (colonIndex < 0) {
                return null;
            }
            int valueStart = colonIndex + 1;
            while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) {
                valueStart++;
            }
            if (valueStart >= json.length()) {
                return null;
            }
            char ch = json.charAt(valueStart);
            if (ch == '"') {
                return readQuotedString(json, valueStart + 1);
            }
            if (ch == '{') {
                int end = findMatching(json, '{', '}', valueStart);
                return end >= valueStart ? json.substring(valueStart, end + 1) : null;
            }
            if (ch == '[') {
                int end = findMatching(json, '[', ']', valueStart);
                return end >= valueStart ? json.substring(valueStart, end + 1) : null;
            }
            int end = valueStart;
            while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}') {
                end++;
            }
            return json.substring(valueStart, end).trim();
        }
        return null;
    }

    private static String readStringField(String json, String key) {
        String value = extractObjectValue(json, key);
        if (value == null) {
            return null;
        }
        if (value.startsWith("\"") && value.endsWith("\"")) {
            return unescapeJsonString(value.substring(1, value.length() - 1));
        }
        return value.trim();
    }

    private static double readDoubleField(String json, String key) {
        String value = readStringField(json, key);
        if (value == null || value.isBlank()) {
            return Double.NaN;
        }
        try {
            return Double.parseDouble(value);
        }
        catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    private static ArrayList<String> readStringArrayItems(String jsonArray) {
        ArrayList<String> values = new ArrayList<>();
        if (jsonArray == null || jsonArray.isBlank()) {
            return values;
        }
        int start = jsonArray.indexOf('[');
        int end = jsonArray.lastIndexOf(']');
        if (start < 0 || end < 0 || end <= start) {
            return values;
        }
        String content = jsonArray.substring(start + 1, end);
        for (String part : content.split(",")) {
            String trimmed = part.trim();
            if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
                values.add(unescapeJsonString(trimmed.substring(1, trimmed.length() - 1)));
            }
        }
        return values;
    }

    private static String readQuotedString(String json, int startIndex) {
        StringBuilder value = new StringBuilder();
        boolean escaped = false;
        for (int i = startIndex; i < json.length(); i++) {
            char ch = json.charAt(i);
            if (escaped) {
                value.append(ch);
                escaped = false;
            }
            else if (ch == '\\') {
                escaped = true;
            }
            else if (ch == '"') {
                return value.toString();
            }
            else {
                value.append(ch);
            }
        }
        return value.toString();
    }

    private static String unescapeJsonString(String value) {
        return value.replace("\\\"", "\"").replace("\\n", " ");
    }

    private static ArrayList<String> splitJsonObjects(String arrayText) {
        ArrayList<String> objects = new ArrayList<>();
        if (arrayText == null || arrayText.isBlank()) {
            return objects;
        }

        int start = arrayText.indexOf('[');
        int end = arrayText.lastIndexOf(']');
        if (start < 0 || end < 0 || end <= start) {
            return objects;
        }

        String content = arrayText.substring(start + 1, end);
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        int objectStart = -1;

        for (int i = 0; i < content.length(); i++) {
            char ch = content.charAt(i);
            if (inString) {
                if (escaped) {
                    escaped = false;
                }
                else if (ch == '\\') {
                    escaped = true;
                }
                else if (ch == '"') {
                    inString = false;
                }
                continue;
            }

            if (ch == '"') {
                inString = true;
            }
            else if (ch == '{') {
                if (depth == 0) {
                    objectStart = i;
                }
                depth++;
            }
            else if (ch == '}') {
                depth--;
                if (depth == 0 && objectStart >= 0) {
                    objects.add(content.substring(objectStart, i + 1));
                    objectStart = -1;
                }
            }
        }

        return objects;
    }

    private static int findMatching(String json, char open, char close, int startIndex) {
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = startIndex; i < json.length(); i++) {
            char ch = json.charAt(i);
            if (inString) {
                if (escaped) {
                    escaped = false;
                }
                else if (ch == '\\') {
                    escaped = true;
                }
                else if (ch == '"') {
                    inString = false;
                }
                continue;
            }
            if (ch == '"') {
                inString = true;
            }
            else if (ch == open) {
                depth++;
            }
            else if (ch == close) {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }
}
