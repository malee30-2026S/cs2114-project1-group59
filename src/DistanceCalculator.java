import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Calculates how far restaurants are from the user, in miles.
 *
 * By default it uses the haversine formula: the straight-line ("as the crow
 * flies") distance between two GPS points. That's free, works offline, and
 * is accurate enough for sorting and "within X miles" filters.
 *
 * If a Google Maps API key is available, either passed to the constructor
 * or set in the GOOGLE_MAPS_API_KEY environment variable, it asks the
 * Google Routes API for the real travel distance instead. If Google can't
 * answer (no internet, bad key, no route), it falls back to the
 * straight-line distance so callers always get a number.
 *
 * @author Jaidev Gogineni
 */
public class DistanceCalculator {
    /** How the user is getting there. Matches the Google Routes API modes. */
    public enum TravelMode {
        DRIVE, WALK, BICYCLE, TRANSIT
    }

    private static final double EARTH_RADIUS_MILES = 3958.8;
    private static final double METERS_PER_MILE = 1609.344;
    private static final String ROUTES_URL =
            "https://routes.googleapis.com/directions/v2:computeRoutes";
    private static final Pattern DISTANCE_METERS =
            Pattern.compile("\"distanceMeters\"\\s*:\\s*(\\d+)");
    // Matches "37.2296, -80.4139" and "37.2296 N, 80.4139 W", with or without degree signs.
    // The degree sign is written as a unicode escape so the file compiles on any encoding.
    private static final Pattern COORDINATES = Pattern.compile(
            "^\\s*([-+]?\\d+(?:\\.\\d+)?)\\s*\u00B0?\\s*([NSns])?\\s*[,\\s]\\s*"
            + "([-+]?\\d+(?:\\.\\d+)?)\\s*\u00B0?\\s*([EWew])?\\s*$");

    private final String apiKey;
    private final HttpClient http;
    private TravelMode travelMode;
    private boolean warnedAboutGoogle;

    /**
     * Creates a calculator that uses Google Maps if the GOOGLE_MAPS_API_KEY
     * environment variable is set, and straight-line distance otherwise.
     */
    public DistanceCalculator() {
        this(System.getenv("GOOGLE_MAPS_API_KEY"));
    }

    /**
     * Creates a calculator with a specific Google Maps API key.
     *
     * @param apiKey a Google Maps Platform key with the Routes API enabled,
     *               or null to always use straight-line distance
     */
    public DistanceCalculator(String apiKey) {
        this.apiKey = apiKey == null || apiKey.isBlank() ? null : apiKey.trim();
        this.http = this.apiKey == null ? null
                : HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        this.travelMode = TravelMode.DRIVE;
    }

    /**
     * @return true if distances come from Google Maps, false if they're
     *         straight-line only
     */
    public boolean isUsingGoogleMaps() {
        return apiKey != null;
    }

    /**
     * @return the travel mode used for Google Maps lookups
     */
    public TravelMode getTravelMode() {
        return travelMode;
    }

    /**
     * Sets how the user travels, e.g. from their profile's mode of
     * transportation. Only affects Google Maps lookups.
     *
     * @param travelMode DRIVE, WALK, BICYCLE, or TRANSIT
     * @throws IllegalArgumentException if travelMode is null
     */
    public void setTravelMode(TravelMode travelMode) {
        if (travelMode == null) {
            throw new IllegalArgumentException("Travel mode cannot be null");
        }
        this.travelMode = travelMode;
    }

    /**
     * Gets the distance between two GPS points in miles.
     *
     * @param fromLat   starting latitude
     * @param fromLng   starting longitude
     * @param toLat     destination latitude
     * @param toLng     destination longitude
     * @return distance in miles
     * @throws IllegalArgumentException if any coordinate is out of range
     */
    public double getDistance(double fromLat, double fromLng, double toLat, double toLng) {
        checkCoordinates(fromLat, fromLng);
        checkCoordinates(toLat, toLng);
        if (apiKey != null) {
            double miles = fetchGoogleDistance(fromLat, fromLng, toLat, toLng);
            if (!Double.isNaN(miles)) {
                return miles;
            }
        }
        return straightLineDistance(fromLat, fromLng, toLat, toLng);
    }

    /**
     * Gets the distance from the user to a restaurant in miles, and stores
     * it on the restaurant so restaurant.getDistance() returns it.
     *
     * @param userLat    the user's latitude
     * @param userLng    the user's longitude
     * @param restaurant the restaurant to measure to
     * @return distance in miles
     * @throws IllegalArgumentException if restaurant is null or has no
     *                                  location, or coordinates are invalid
     */
    public double getDistance(double userLat, double userLng, Restaurant restaurant) {
        if (restaurant == null) {
            throw new IllegalArgumentException("Restaurant cannot be null");
        }
        if (!restaurant.hasLocation()) {
            throw new IllegalArgumentException(restaurant.getName() + " has no location");
        }
        double miles = getDistance(userLat, userLng,
                restaurant.getLatitude(), restaurant.getLongitude());
        restaurant.setDistance(miles);
        return miles;
    }

    /**
     * Fills in getDistance() for every restaurant in the list. Restaurants
     * with no location are skipped and keep a distance of NaN.
     *
     * With Google Maps turned on this makes one request per restaurant, so
     * run it on a filtered list rather than the whole dataset.
     *
     * @param userLat     the user's latitude
     * @param userLng     the user's longitude
     * @param restaurants the restaurants to update
     * @throws IllegalArgumentException if restaurants is null or the user's
     *                                  coordinates are invalid
     */
    public void updateDistances(double userLat, double userLng, ArrayList<Restaurant> restaurants) {
        if (restaurants == null) {
            throw new IllegalArgumentException("Restaurant list cannot be null");
        }
        checkCoordinates(userLat, userLng);
        for (Restaurant restaurant : restaurants) {
            if (restaurant != null && restaurant.hasLocation()) {
                getDistance(userLat, userLng, restaurant);
            }
        }
    }

    /**
     * Straight-line distance between two GPS points using the haversine
     * formula. Never touches the internet.
     *
     * @return distance in miles
     * @throws IllegalArgumentException if any coordinate is out of range
     */
    public static double straightLineDistance(double lat1, double lng1, double lat2, double lng2) {
        checkCoordinates(lat1, lng1);
        checkCoordinates(lat2, lng2);
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return EARTH_RADIUS_MILES * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /**
     * Turns a location string into {latitude, longitude}. Accepts either
     * "37.2296, -80.4139" or "37.2296 N, 80.4139 W" (degree signs are optional).
     *
     * @param location the location text
     * @return a two-element array: {latitude, longitude}
     * @throws IllegalArgumentException if the text can't be parsed or the
     *                                  coordinates are out of range
     */
    public static double[] parseCoordinates(String location) {
        if (location == null) {
            throw new IllegalArgumentException("Location cannot be null");
        }
        Matcher match = COORDINATES.matcher(location);
        if (!match.matches()) {
            throw new IllegalArgumentException("Can't read location: \"" + location + "\"");
        }
        double lat = Double.parseDouble(match.group(1));
        double lng = Double.parseDouble(match.group(3));
        if (match.group(2) != null) {
            lat = match.group(2).equalsIgnoreCase("S") ? -Math.abs(lat) : Math.abs(lat);
        }
        if (match.group(4) != null) {
            lng = match.group(4).equalsIgnoreCase("W") ? -Math.abs(lng) : Math.abs(lng);
        }
        checkCoordinates(lat, lng);
        return new double[] {lat, lng};
    }

    /**
     * Checks that a latitude/longitude pair is a real place on Earth.
     *
     * @throws IllegalArgumentException if latitude isn't -90 to 90 or
     *                                  longitude isn't -180 to 180
     */
    public static void checkCoordinates(double latitude, double longitude) {
        if (Double.isNaN(latitude) || latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90, got " + latitude);
        }
        if (Double.isNaN(longitude) || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180, got " + longitude);
        }
    }

    /**
     * Asks the Google Routes API for the travel distance.
     *
     * @return distance in miles, or NaN if Google couldn't answer
     */
    private double fetchGoogleDistance(double fromLat, double fromLng, double toLat, double toLng) {
        String body = String.format(Locale.ROOT,
                "{\"origin\":{\"location\":{\"latLng\":{\"latitude\":%f,\"longitude\":%f}}},"
                + "\"destination\":{\"location\":{\"latLng\":{\"latitude\":%f,\"longitude\":%f}}},"
                + "\"travelMode\":\"%s\"}",
                fromLat, fromLng, toLat, toLng, travelMode);
        HttpRequest request = HttpRequest.newBuilder(URI.create(ROUTES_URL))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .header("X-Goog-Api-Key", apiKey)
                .header("X-Goog-FieldMask", "routes.distanceMeters")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                warnOnce("Google Maps returned HTTP " + response.statusCode() + ": " + response.body());
                return Double.NaN;
            }
            Matcher match = DISTANCE_METERS.matcher(response.body());
            return match.find() ? Long.parseLong(match.group(1)) / METERS_PER_MILE : Double.NaN;
        }
        catch (IOException e) {
            warnOnce("Couldn't reach Google Maps: " + e.getMessage());
            return Double.NaN;
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Double.NaN;
        }
    }

    private void warnOnce(String message) {
        if (!warnedAboutGoogle) {
            warnedAboutGoogle = true;
            System.err.println("DistanceCalculator: " + message
                    + " (using straight-line distance instead)");
        }
    }
}
