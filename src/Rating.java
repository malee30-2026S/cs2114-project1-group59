/**
 * One answer to the post-visit survey: how many stars a user gave a
 * restaurant, their review, and when.
 *
 * @author Jaidev Gogineni
 */
public class Rating {
    private final String restaurantName;
    private final String restaurantAddress;
    private final int stars;
    private final String review;
    private final String date;

    /**
     * @param restaurantName    the restaurant's name
     * @param restaurantAddress its address, or "" if unknown
     * @param stars             1 to 5
     * @param review            the review text, or "" for none
     * @param date              when it was rated, e.g. "2026-09-23"
     * @throws IllegalArgumentException if the name is blank or stars isn't 1-5
     */
    public Rating(String restaurantName, String restaurantAddress, int stars, String review, String date) {
        if (restaurantName == null || restaurantName.isBlank()) {
            throw new IllegalArgumentException("Restaurant name cannot be empty");
        }
        if (stars < 1 || stars > 5) {
            throw new IllegalArgumentException("Stars must be 1 to 5");
        }
        this.restaurantName = restaurantName.trim();
        this.restaurantAddress = restaurantAddress == null ? "" : restaurantAddress.trim();
        this.stars = stars;
        this.review = review == null ? "" : review.trim();
        this.date = date == null ? "" : date.trim();
    }

    public String getRestaurantName() {
        return restaurantName;
    }

    public String getRestaurantAddress() {
        return restaurantAddress;
    }

    public int getStars() {
        return stars;
    }

    public String getReview() {
        return review;
    }

    /**
     * @return the day it was rated, e.g. "2026-09-23"
     */
    public String getDate() {
        return date.length() >= 10 ? date.substring(0, 10) : date;
    }

    @Override
    public String toString() {
        return restaurantName + ": " + stars + "/5" + (review.isEmpty() ? "" : " \"" + review + "\"");
    }
}
