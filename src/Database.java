import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Saves restaurants, tags, and user profiles in a SQLite database file so
 * the data is still there the next time the app starts. Data loads and
 * cleans the open dataset, then hands the Restaurant objects here to store.
 *
 * Every query uses a PreparedStatement, so user input is always treated as
 * plain text and never run as SQL. That's what stops SQL injection:
 * findRestaurant("x'; DROP TABLE restaurants; --") just finds nothing and
 * returns null.
 *
 * Needs the SQLite driver jar (lib/sqlite-jdbc-*.jar) on the classpath.
 * Close the database when you're done, or use try-with-resources:
 *
 *     try (Database db = new Database("hungryhokie.db")) {
 *         db.saveRestaurant(restaurant);
 *     }
 *
 * Database problems (missing driver, locked file, disk full) are thrown as
 * IllegalStateException. Bad arguments are IllegalArgumentException.
 *
 * @author Jaidev Gogineni
 */
public class Database implements AutoCloseable {
    private static final String RESTAURANT_COLUMNS =
            "r.id, r.name, r.address, r.price_level, r.latitude, r.longitude";
    // Which list a row in profile_restaurants belongs to
    private static final String FAVORITES = "favorite";
    private static final String HIDDEN = "hidden";
    private static final String PENDING = "pending";
    private static final String LAST_USER = "last_user";

    private final Connection connection;

    /**
     * Opens the database file, creating it and its tables if needed.
     *
     * @param filePath path to the database file, e.g. "hungryhokie.db"
     * @throws IllegalArgumentException if filePath is blank
     * @throws IllegalStateException    if the database can't be opened
     */
    public Database(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("Database file path cannot be empty");
        }
        Connection opened = null;
        try {
            opened = DriverManager.getConnection("jdbc:sqlite:" + filePath);
            createTables(opened);
        }
        catch (SQLException e) {
            closeQuietly(opened);
            String hint = e.getMessage() != null && e.getMessage().contains("No suitable driver")
                    ? " (is lib/sqlite-jdbc-*.jar on the classpath?)" : "";
            throw new IllegalStateException("Could not open database " + filePath + hint, e);
        }
        connection = opened;
    }

    // ---------------------------------------------------------------- Restaurants

    /**
     * Saves a restaurant with its tags, reviews, and menu. If a restaurant
     * with the same name and address is already saved, it's replaced.
     *
     * @param restaurant the restaurant to save
     * @throws IllegalArgumentException if restaurant is null
     */
    public void saveRestaurant(Restaurant restaurant) {
        if (restaurant == null) {
            throw new IllegalArgumentException("Restaurant cannot be null");
        }
        inTransaction(() -> {
            long id = upsertRestaurant(restaurant);
            for (String table : new String[] {"restaurant_tags", "reviews", "menu_items"}) {
                update("DELETE FROM " + table + " WHERE restaurant_id = ?", id);
            }
            for (Tag tag : restaurant.getTags()) {
                update("INSERT OR IGNORE INTO restaurant_tags (restaurant_id, tag_id) VALUES (?, ?)",
                        id, tagId(tag));
            }
            for (String review : restaurant.getReviews()) {
                update("INSERT INTO reviews (restaurant_id, review) VALUES (?, ?)", id, review);
            }
            for (String item : restaurant.getMenu()) {
                update("INSERT INTO menu_items (restaurant_id, item) VALUES (?, ?)", id, item);
            }
        });
    }

    /**
     * @return every saved restaurant, sorted by name
     */
    public ArrayList<Restaurant> getAllRestaurants() {
        return queryRestaurants("SELECT " + RESTAURANT_COLUMNS
                + " FROM restaurants r ORDER BY r.name");
    }

    /**
     * Finds a restaurant by name, ignoring case. If several locations share
     * the name, returns the first one saved.
     *
     * @param name the restaurant's name
     * @return the restaurant, or null if there's no match or name is blank
     */
    public Restaurant findRestaurant(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        ArrayList<Restaurant> found = queryRestaurants("SELECT " + RESTAURANT_COLUMNS
                + " FROM restaurants r WHERE r.name = ? ORDER BY r.id LIMIT 1", name.trim());
        return found.isEmpty() ? null : found.get(0);
    }

    /**
     * @param tag the tag to look for
     * @return every saved restaurant with that tag, sorted by name
     * @throws IllegalArgumentException if tag is null
     */
    public ArrayList<Restaurant> getRestaurantsByTag(Tag tag) {
        if (tag == null) {
            throw new IllegalArgumentException("Tag cannot be null");
        }
        return queryRestaurants("SELECT " + RESTAURANT_COLUMNS + " FROM restaurants r"
                + " JOIN restaurant_tags rt ON rt.restaurant_id = r.id"
                + " JOIN tags t ON t.id = rt.tag_id"
                + " WHERE t.name = ? ORDER BY r.name", tag.getName());
    }

    /**
     * Deletes a restaurant along with its tags, reviews, and menu.
     *
     * @param restaurant the restaurant to delete (matched by name and address)
     * @return true if it was deleted, false if it wasn't saved
     * @throws IllegalArgumentException if restaurant is null
     */
    public boolean removeRestaurant(Restaurant restaurant) {
        if (restaurant == null) {
            throw new IllegalArgumentException("Restaurant cannot be null");
        }
        try {
            return update("DELETE FROM restaurants WHERE name = ? AND address = ?",
                    restaurant.getName(), restaurant.getAddress()) > 0;
        }
        catch (SQLException e) {
            throw error(e);
        }
    }

    /**
     * @return every tag that's been saved, sorted by name
     */
    public ArrayList<Tag> getAllTags() {
        ArrayList<Tag> tags = new ArrayList<>();
        try (PreparedStatement query = prepare("SELECT name FROM tags ORDER BY name");
                ResultSet rows = query.executeQuery()) {
            while (rows.next()) {
                tags.add(new Tag(rows.getString("name")));
            }
        }
        catch (SQLException e) {
            throw error(e);
        }
        return tags;
    }

    // ---------------------------------------------------------------- Profiles

    /**
     * Saves a user profile, or updates the name if the email already exists.
     * Matches Profile(String name, String email).
     *
     * @param name  the user's name
     * @param email the user's email (their unique ID)
     * @throws IllegalArgumentException if name or email is blank
     */
    public void saveProfile(String name, String email) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }
        checkEmail(email);
        try {
            update("INSERT INTO profiles (email, name) VALUES (?, ?)"
                    + " ON CONFLICT (email) DO UPDATE SET name = excluded.name",
                    email.trim(), name.trim());
        }
        catch (SQLException e) {
            throw error(e);
        }
    }

    /**
     * @param email the user's email
     * @return the saved name for that email, or null if there's no profile
     */
    public String getProfileName(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        try (PreparedStatement query = prepare("SELECT name FROM profiles WHERE email = ?", email.trim());
                ResultSet rows = query.executeQuery()) {
            return rows.next() ? rows.getString("name") : null;
        }
        catch (SQLException e) {
            throw error(e);
        }
    }

    /**
     * @param email the user's email
     * @return true if a profile with that email is saved
     */
    public boolean profileExists(String email) {
        return getProfileName(email) != null;
    }

    /**
     * Adds to (or subtracts from) how much a user likes a tag. Use a
     * positive change after a good rating and a negative one after a bad
     * rating. Weights start at 0.
     *
     * @param email  the user's email
     * @param tag    the tag, e.g. Italian
     * @param change how much to add; negative lowers the weight
     * @throws IllegalArgumentException if there's no profile for the email,
     *                                  tag is null, or change isn't a number
     */
    public void adjustTagWeight(String email, Tag tag, double change) {
        checkEmail(email);
        if (tag == null) {
            throw new IllegalArgumentException("Tag cannot be null");
        }
        if (Double.isNaN(change) || Double.isInfinite(change)) {
            throw new IllegalArgumentException("Weight change must be a number");
        }
        if (!profileExists(email)) {
            throw new IllegalArgumentException("No profile saved for " + email);
        }
        inTransaction(() -> update("INSERT INTO tag_weights (email, tag_id, weight) VALUES (?, ?, ?)"
                + " ON CONFLICT (email, tag_id) DO UPDATE SET weight = weight + excluded.weight",
                email.trim(), tagId(tag), change));
    }

    /**
     * @param email the user's email
     * @return each tag the user has a weight for, mapped to that weight
     *         (empty if there's no profile or no weights yet)
     */
    public HashMap<Tag, Double> getTagWeights(String email) {
        HashMap<Tag, Double> weights = new HashMap<>();
        if (email == null || email.isBlank()) {
            return weights;
        }
        try (PreparedStatement query = prepare("SELECT t.name, w.weight FROM tag_weights w"
                + " JOIN tags t ON t.id = w.tag_id WHERE w.email = ?", email.trim());
                ResultSet rows = query.executeQuery()) {
            while (rows.next()) {
                weights.put(new Tag(rows.getString("name")), rows.getDouble("weight"));
            }
        }
        catch (SQLException e) {
            throw error(e);
        }
        return weights;
    }

    /**
     * Saves everything in a profile: name, age, location, dietary
     * restrictions, favorite cuisines, taste tags, learned tag weights,
     * favorite restaurants, and hidden restaurants and tags. Replaces what
     * was saved before for that email. Pending visits and ratings are kept.
     *
     * @param profile the profile to save
     * @throws IllegalArgumentException if profile is null or has no name or
     *                                  email
     */
    public void saveProfile(Profile profile) {
        if (profile == null) {
            throw new IllegalArgumentException("Profile cannot be null");
        }
        if (profile.getName().isBlank()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }
        checkEmail(profile.getEmail());
        String email = profile.getEmail().trim();
        inTransaction(() -> {
            update("INSERT INTO profiles (email, name, age, location) VALUES (?, ?, ?, ?)"
                    + " ON CONFLICT (email) DO UPDATE SET name = excluded.name,"
                    + " age = excluded.age, location = excluded.location",
                    email, profile.getName(), profile.getAge(), profile.getLocation());
            for (String table : new String[] {"profile_diets", "profile_cuisines", "profile_tastes",
                "profile_hidden_tags", "tag_weights"}) {
                update("DELETE FROM " + table + " WHERE email = ?", email);
            }
            update("DELETE FROM profile_restaurants WHERE email = ? AND list IN (?, ?)", email, FAVORITES, HIDDEN);

            for (String diet : profile.getDietaryRestrictions()) {
                update("INSERT OR IGNORE INTO profile_diets (email, restriction) VALUES (?, ?)", email, diet);
            }
            for (String cuisine : profile.getFavoriteCuisines()) {
                update("INSERT OR IGNORE INTO profile_cuisines (email, cuisine) VALUES (?, ?)", email, cuisine);
            }
            for (Tag tag : profile.getTasteProfile()) {
                update("INSERT OR IGNORE INTO profile_tastes (email, tag_id) VALUES (?, ?)", email, tagId(tag));
            }
            for (Tag tag : profile.getBlacklist().getBlacklistedTags()) {
                update("INSERT OR IGNORE INTO profile_hidden_tags (email, tag_id) VALUES (?, ?)", email, tagId(tag));
            }
            for (java.util.Map.Entry<Tag, Double> weight : profile.getTagWeights().entrySet()) {
                update("INSERT OR REPLACE INTO tag_weights (email, tag_id, weight) VALUES (?, ?, ?)",
                        email, tagId(weight.getKey()), weight.getValue());
            }
            for (Restaurant r : profile.getFavoriteRestaurants()) {
                insertListEntry(email, FAVORITES, r);
            }
            for (Restaurant r : profile.getBlacklist().getBlacklistedRestaurants()) {
                insertListEntry(email, HIDDEN, r);
            }
        });
    }

    /**
     * Loads everything saveProfile(Profile) saved. Favorite, hidden, and
     * pending restaurants come back with just their name and address, which
     * is enough to match them with Restaurant.equals.
     *
     * @param email the user's email
     * @return the saved profile, or null if there isn't one
     */
    public Profile loadProfile(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        String key = email.trim();
        try {
            Profile profile;
            try (PreparedStatement query = prepare(
                    "SELECT email, name, age, location FROM profiles WHERE email = ?", key);
                    ResultSet rows = query.executeQuery()) {
                if (!rows.next()) {
                    return null;
                }
                profile = new Profile(rows.getString("name"), rows.getInt("age"), rows.getString("location"));
                profile.setEmail(rows.getString("email"));
            }
            for (String diet : strings("SELECT restriction FROM profile_diets WHERE email = ? ORDER BY rowid", key)) {
                profile.addDietaryRestriction(diet);
            }
            for (String cuisine : strings("SELECT cuisine FROM profile_cuisines WHERE email = ? ORDER BY rowid", key)) {
                profile.addFavoriteCuisine(cuisine);
            }
            for (String tag : strings("SELECT t.name FROM profile_tastes p JOIN tags t ON t.id = p.tag_id"
                    + " WHERE p.email = ? ORDER BY p.rowid", key)) {
                profile.addTasteTag(new Tag(tag));
            }
            for (String tag : strings("SELECT t.name FROM profile_hidden_tags h JOIN tags t ON t.id = h.tag_id"
                    + " WHERE h.email = ? ORDER BY h.rowid", key)) {
                profile.addToBlacklistTag(new Tag(tag));
            }
            getTagWeights(key).forEach(profile::addTagWeight);
            for (Restaurant r : listEntries(key, FAVORITES)) {
                profile.addFavoriteRestaurant(r);
            }
            for (Restaurant r : listEntries(key, HIDDEN)) {
                profile.addToBlacklist(r);
            }
            return profile;
        }
        catch (SQLException e) {
            throw error(e);
        }
    }

    // ---------------------------------------------------------------- Visits and ratings

    /**
     * Remembers that a user went to eat somewhere, so the app can ask how it
     * was the next time it opens. Adding the same visit twice does nothing.
     *
     * @throws IllegalArgumentException if restaurant is null or there's no
     *                                  profile for the email
     */
    public void addPendingVisit(String email, Restaurant restaurant) {
        requireProfile(email);
        if (restaurant == null) {
            throw new IllegalArgumentException("Restaurant cannot be null");
        }
        try {
            insertListEntry(email.trim(), PENDING, restaurant);
        }
        catch (SQLException e) {
            throw error(e);
        }
    }

    /**
     * @return restaurants the user went to but hasn't rated yet, oldest
     *         first (empty if none)
     */
    public ArrayList<Restaurant> getPendingVisits(String email) {
        if (email == null || email.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return listEntries(email.trim(), PENDING);
        }
        catch (SQLException e) {
            throw error(e);
        }
    }

    /**
     * @return true if the visit was pending and is now removed
     */
    public boolean removePendingVisit(String email, Restaurant restaurant) {
        if (email == null || email.isBlank() || restaurant == null) {
            return false;
        }
        try {
            return update("DELETE FROM profile_restaurants WHERE email = ? AND list = ? AND name = ? AND address = ?",
                    email.trim(), PENDING, restaurant.getName(), restaurant.getAddress()) > 0;
        }
        catch (SQLException e) {
            throw error(e);
        }
    }

    /**
     * Saves a post-visit rating to the user's history.
     *
     * @param stars  1 to 5
     * @param review the review, or "" for none
     * @throws IllegalArgumentException if stars isn't 1-5, restaurant is null,
     *                                  or there's no profile for the email
     */
    public void saveRating(String email, Restaurant restaurant, int stars, String review) {
        requireProfile(email);
        if (restaurant == null) {
            throw new IllegalArgumentException("Restaurant cannot be null");
        }
        if (stars < 1 || stars > 5) {
            throw new IllegalArgumentException("Stars must be 1 to 5");
        }
        try {
            update("INSERT INTO ratings (email, name, address, stars, review) VALUES (?, ?, ?, ?, ?)",
                    email.trim(), restaurant.getName(), restaurant.getAddress(), stars,
                    review == null ? "" : review.trim());
        }
        catch (SQLException e) {
            throw error(e);
        }
    }

    /**
     * @return the user's ratings, newest first (empty if none)
     */
    public ArrayList<Rating> getRatings(String email) {
        ArrayList<Rating> ratings = new ArrayList<>();
        if (email == null || email.isBlank()) {
            return ratings;
        }
        try (PreparedStatement query = prepare("SELECT name, address, stars, review, rated_at FROM ratings"
                + " WHERE email = ? ORDER BY rated_at DESC, rowid DESC", email.trim());
                ResultSet rows = query.executeQuery()) {
            while (rows.next()) {
                ratings.add(new Rating(rows.getString("name"), rows.getString("address"),
                        rows.getInt("stars"), rows.getString("review"), rows.getString("rated_at")));
            }
        }
        catch (SQLException e) {
            throw error(e);
        }
        return ratings;
    }

    // ---------------------------------------------------------------- Signed-in user

    /**
     * Remembers who's signed in, so the app can skip the survey next time.
     *
     * @throws IllegalArgumentException if there's no profile for the email
     */
    public void setLastUser(String email) {
        requireProfile(email);
        try {
            update("INSERT OR REPLACE INTO app_settings (key, value) VALUES (?, ?)", LAST_USER, email.trim());
        }
        catch (SQLException e) {
            throw error(e);
        }
    }

    /**
     * @return the email of whoever was signed in last, or null if nobody
     */
    public String getLastUser() {
        try {
            ArrayList<String> values = strings("SELECT value FROM app_settings WHERE key = ?", LAST_USER);
            return values.isEmpty() ? null : values.get(0);
        }
        catch (SQLException e) {
            throw error(e);
        }
    }

    /**
     * Signs the user out, so the app shows the survey next time.
     */
    public void clearLastUser() {
        try {
            update("DELETE FROM app_settings WHERE key = ?", LAST_USER);
        }
        catch (SQLException e) {
            throw error(e);
        }
    }

    /**
     * Closes the database file. Safe to call more than once.
     */
    @Override
    public void close() {
        closeQuietly(connection);
    }

    // ---------------------------------------------------------------- Helpers

    /** A block of database work that may throw SQLException. */
    private interface SqlWork {
        void run() throws SQLException;
    }

    private static void createTables(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("CREATE TABLE IF NOT EXISTS restaurants ("
                    + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + " name TEXT NOT NULL COLLATE NOCASE,"
                    + " address TEXT NOT NULL DEFAULT '' COLLATE NOCASE,"
                    + " price_level INTEGER NOT NULL DEFAULT 0,"
                    + " latitude REAL,"
                    + " longitude REAL,"
                    + " UNIQUE (name, address))");
            statement.execute("CREATE TABLE IF NOT EXISTS tags ("
                    + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + " name TEXT NOT NULL UNIQUE COLLATE NOCASE)");
            statement.execute("CREATE TABLE IF NOT EXISTS restaurant_tags ("
                    + " restaurant_id INTEGER NOT NULL REFERENCES restaurants(id) ON DELETE CASCADE,"
                    + " tag_id INTEGER NOT NULL REFERENCES tags(id) ON DELETE CASCADE,"
                    + " PRIMARY KEY (restaurant_id, tag_id))");
            statement.execute("CREATE TABLE IF NOT EXISTS reviews ("
                    + " restaurant_id INTEGER NOT NULL REFERENCES restaurants(id) ON DELETE CASCADE,"
                    + " review TEXT NOT NULL)");
            statement.execute("CREATE TABLE IF NOT EXISTS menu_items ("
                    + " restaurant_id INTEGER NOT NULL REFERENCES restaurants(id) ON DELETE CASCADE,"
                    + " item TEXT NOT NULL)");
            statement.execute("CREATE TABLE IF NOT EXISTS profiles ("
                    + " email TEXT PRIMARY KEY COLLATE NOCASE,"
                    + " name TEXT NOT NULL)");
            statement.execute("CREATE TABLE IF NOT EXISTS tag_weights ("
                    + " email TEXT NOT NULL COLLATE NOCASE REFERENCES profiles(email) ON DELETE CASCADE,"
                    + " tag_id INTEGER NOT NULL REFERENCES tags(id) ON DELETE CASCADE,"
                    + " weight REAL NOT NULL DEFAULT 0,"
                    + " PRIMARY KEY (email, tag_id))");
        }
        // Databases made before profiles had these columns get them added
        addColumnIfMissing(connection, "profiles", "age", "INTEGER NOT NULL DEFAULT 0");
        addColumnIfMissing(connection, "profiles", "location", "TEXT NOT NULL DEFAULT ''");
        String profileKey = " email TEXT NOT NULL COLLATE NOCASE REFERENCES profiles(email) ON DELETE CASCADE,";
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS profile_diets (" + profileKey
                    + " restriction TEXT NOT NULL COLLATE NOCASE,"
                    + " PRIMARY KEY (email, restriction))");
            statement.execute("CREATE TABLE IF NOT EXISTS profile_cuisines (" + profileKey
                    + " cuisine TEXT NOT NULL COLLATE NOCASE,"
                    + " PRIMARY KEY (email, cuisine))");
            statement.execute("CREATE TABLE IF NOT EXISTS profile_tastes (" + profileKey
                    + " tag_id INTEGER NOT NULL REFERENCES tags(id) ON DELETE CASCADE,"
                    + " PRIMARY KEY (email, tag_id))");
            statement.execute("CREATE TABLE IF NOT EXISTS profile_hidden_tags (" + profileKey
                    + " tag_id INTEGER NOT NULL REFERENCES tags(id) ON DELETE CASCADE,"
                    + " PRIMARY KEY (email, tag_id))");
            // Favorite, hidden, and pending-visit restaurants, by name and address
            statement.execute("CREATE TABLE IF NOT EXISTS profile_restaurants (" + profileKey
                    + " list TEXT NOT NULL,"
                    + " name TEXT NOT NULL COLLATE NOCASE,"
                    + " address TEXT NOT NULL DEFAULT '' COLLATE NOCASE,"
                    + " added_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                    + " PRIMARY KEY (email, list, name, address))");
            statement.execute("CREATE TABLE IF NOT EXISTS ratings (" + profileKey
                    + " name TEXT NOT NULL,"
                    + " address TEXT NOT NULL DEFAULT '',"
                    + " stars INTEGER NOT NULL CHECK (stars BETWEEN 1 AND 5),"
                    + " review TEXT NOT NULL DEFAULT '',"
                    + " rated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            statement.execute("CREATE TABLE IF NOT EXISTS app_settings ("
                    + " key TEXT PRIMARY KEY,"
                    + " value TEXT NOT NULL)");
        }
    }

    private static void addColumnIfMissing(Connection connection, String table, String column,
            String definition) throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet columns = statement.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (columns.next()) {
                if (column.equalsIgnoreCase(columns.getString("name"))) {
                    return;
                }
            }
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        }
    }

    /** Throws if there's no saved profile for the email. */
    private void requireProfile(String email) {
        checkEmail(email);
        if (!profileExists(email)) {
            throw new IllegalArgumentException("No profile saved for " + email);
        }
    }

    private void insertListEntry(String email, String list, Restaurant restaurant) throws SQLException {
        update("INSERT OR IGNORE INTO profile_restaurants (email, list, name, address) VALUES (?, ?, ?, ?)",
                email, list, restaurant.getName(), restaurant.getAddress());
    }

    /** Restaurants on one of a user's lists, with just name and address filled in. */
    private ArrayList<Restaurant> listEntries(String email, String list) throws SQLException {
        ArrayList<Restaurant> restaurants = new ArrayList<>();
        try (PreparedStatement query = prepare("SELECT name, address FROM profile_restaurants"
                + " WHERE email = ? AND list = ? ORDER BY added_at, rowid", email, list);
                ResultSet rows = query.executeQuery()) {
            while (rows.next()) {
                restaurants.add(new Restaurant(rows.getString("name"), new ArrayList<>(),
                        Restaurant.PRICE_UNKNOWN, Double.NaN, Double.NaN, rows.getString("address")));
            }
        }
        return restaurants;
    }

    /** Runs a query and returns the first column of every row. */
    private ArrayList<String> strings(String sql, Object... params) throws SQLException {
        ArrayList<String> values = new ArrayList<>();
        try (PreparedStatement query = prepare(sql, params);
                ResultSet rows = query.executeQuery()) {
            while (rows.next()) {
                values.add(rows.getString(1));
            }
        }
        return values;
    }

    /** Inserts or updates the restaurant's own row and returns its id. */
    private long upsertRestaurant(Restaurant restaurant) throws SQLException {
        Double lat = restaurant.hasLocation() ? restaurant.getLatitude() : null;
        Double lng = restaurant.hasLocation() ? restaurant.getLongitude() : null;
        update("INSERT INTO restaurants (name, address, price_level, latitude, longitude)"
                + " VALUES (?, ?, ?, ?, ?)"
                + " ON CONFLICT (name, address) DO UPDATE SET name = excluded.name,"
                + " price_level = excluded.price_level,"
                + " latitude = excluded.latitude, longitude = excluded.longitude",
                restaurant.getName(), restaurant.getAddress(), restaurant.getPriceLevel(), lat, lng);
        try (PreparedStatement query = prepare("SELECT id FROM restaurants WHERE name = ? AND address = ?",
                restaurant.getName(), restaurant.getAddress());
                ResultSet rows = query.executeQuery()) {
            rows.next();
            return rows.getLong("id");
        }
    }

    /** Returns the tag's id, saving the tag first if it's new. */
    private long tagId(Tag tag) throws SQLException {
        update("INSERT OR IGNORE INTO tags (name) VALUES (?)", tag.getName());
        try (PreparedStatement query = prepare("SELECT id FROM tags WHERE name = ?", tag.getName());
                ResultSet rows = query.executeQuery()) {
            rows.next();
            return rows.getLong("id");
        }
    }

    /** Runs a SELECT on restaurants and builds full Restaurant objects. */
    private ArrayList<Restaurant> queryRestaurants(String sql, Object... params) {
        ArrayList<Long> ids = new ArrayList<>();
        ArrayList<Restaurant> restaurants = new ArrayList<>();
        try {
            try (PreparedStatement query = prepare(sql, params);
                    ResultSet rows = query.executeQuery()) {
                while (rows.next()) {
                    boolean hasLocation = rows.getObject("latitude") != null
                            && rows.getObject("longitude") != null;
                    ids.add(rows.getLong("id"));
                    restaurants.add(new Restaurant(rows.getString("name"), new ArrayList<>(),
                            rows.getInt("price_level"),
                            hasLocation ? rows.getDouble("latitude") : Double.NaN,
                            hasLocation ? rows.getDouble("longitude") : Double.NaN,
                            rows.getString("address")));
                }
            }
            for (int i = 0; i < restaurants.size(); i++) {
                loadDetails(ids.get(i), restaurants.get(i));
            }
        }
        catch (SQLException e) {
            throw error(e);
        }
        return restaurants;
    }

    /** Fills in a restaurant's tags, reviews, and menu. */
    private void loadDetails(long id, Restaurant restaurant) throws SQLException {
        try (PreparedStatement query = prepare("SELECT t.name FROM restaurant_tags rt"
                + " JOIN tags t ON t.id = rt.tag_id WHERE rt.restaurant_id = ? ORDER BY t.name", id);
                ResultSet rows = query.executeQuery()) {
            while (rows.next()) {
                restaurant.addTag(new Tag(rows.getString(1)));
            }
        }
        try (PreparedStatement query = prepare(
                "SELECT review FROM reviews WHERE restaurant_id = ? ORDER BY rowid", id);
                ResultSet rows = query.executeQuery()) {
            while (rows.next()) {
                restaurant.addReview(rows.getString(1));
            }
        }
        try (PreparedStatement query = prepare(
                "SELECT item FROM menu_items WHERE restaurant_id = ? ORDER BY rowid", id);
                ResultSet rows = query.executeQuery()) {
            while (rows.next()) {
                restaurant.addMenuItem(rows.getString(1));
            }
        }
    }

    /** Builds a PreparedStatement with the ? placeholders filled in. */
    private PreparedStatement prepare(String sql, Object... params) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(sql);
        try {
            for (int i = 0; i < params.length; i++) {
                if (params[i] == null) {
                    statement.setNull(i + 1, Types.NULL);
                }
                else {
                    statement.setObject(i + 1, params[i]);
                }
            }
        }
        catch (SQLException e) {
            statement.close();
            throw e;
        }
        return statement;
    }

    /** Runs an INSERT/UPDATE/DELETE and returns how many rows changed. */
    private int update(String sql, Object... params) throws SQLException {
        try (PreparedStatement statement = prepare(sql, params)) {
            return statement.executeUpdate();
        }
    }

    /** Runs work so it either fully saves or, if anything fails, not at all. */
    private void inTransaction(SqlWork work) {
        try {
            connection.setAutoCommit(false);
            try {
                work.run();
                connection.commit();
            }
            catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
            finally {
                connection.setAutoCommit(true);
            }
        }
        catch (SQLException e) {
            throw error(e);
        }
    }

    private static void checkEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be empty");
        }
    }

    private static IllegalStateException error(SQLException e) {
        return new IllegalStateException("Database error: " + e.getMessage(), e);
    }

    private static void closeQuietly(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            }
            catch (SQLException e) {
                // Nothing useful to do if closing fails
            }
        }
    }
}
