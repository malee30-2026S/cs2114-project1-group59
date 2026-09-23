import java.util.Locale;

/**
 * A quality that describes a restaurant, such as a cuisine ("Italian"), a
 * diet ("Vegan"), or a priority ("Quick service"). Restaurants carry tags,
 * and user preferences are tracked as weights on tags.
 *
 * Two tags are equal when their names match, ignoring case, so
 * new Tag("mexican") and new Tag("Mexican") are the same tag.
 *
 * @author Jaidev Gogineni
 */
public class Tag {
    private final String name;

    /**
     * Creates a tag.
     *
     * @param name the tag's name, e.g. "Mexican"
     * @throws IllegalArgumentException if name is null or blank
     */
    public Tag(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tag name cannot be empty");
        }
        this.name = name.trim();
    }

    /**
     * @return the tag's name
     */
    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Tag)) {
            return false;
        }
        return name.equalsIgnoreCase(((Tag) other).name);
    }

    @Override
    public int hashCode() {
        return name.toLowerCase(Locale.ROOT).hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}
