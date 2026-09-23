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
