import java.util.ArrayList;

/**
 * Collects the onboarding answers used to build a user profile.
 */
public class Survey {
    private String name;
    private String email;
    private int age;
    private String location;
    private final ArrayList<String> dietaryRestrictions;
    private final ArrayList<Tag> tags;

    public Survey() {
        this("", 0, "");
    }

    public Survey(String name, int age, String location) {
        this.name = name == null ? "" : name.trim();
        this.email = "";
        this.age = age;
        this.location = location == null ? "" : location.trim();
        this.dietaryRestrictions = new ArrayList<>();
        this.tags = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null ? "" : name.trim();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email == null ? "" : email.trim();
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location == null ? "" : location.trim();
    }

    public void addDietaryRestriction(String restriction) {
        if (restriction == null || restriction.isBlank()) {
            throw new IllegalArgumentException("Dietary restriction cannot be empty");
        }
        String normalized = restriction.trim();
        if (!dietaryRestrictions.contains(normalized)) {
            dietaryRestrictions.add(normalized);
        }
    }

    public ArrayList<String> getDietaryRestrictions() {
        return new ArrayList<>(dietaryRestrictions);
    }

    public void addTag(Tag tag) {
        if (tag == null) {
            throw new IllegalArgumentException("Tag cannot be null");
        }
        if (!tags.contains(tag)) {
            tags.add(tag);
        }
    }

    public void addTag(String tagName) {
        if (tagName == null || tagName.isBlank()) {
            throw new IllegalArgumentException("Tag name cannot be empty");
        }
        addTag(new Tag(tagName));
    }

    public ArrayList<Tag> getTags() {
        return new ArrayList<>(tags);
    }

    /**
     * Converts the survey answers into a profile.
     *
     * @return a new Profile generated from this survey
     */
    public Profile toProfile() {
        Profile profile = new Profile(name, age, location);
        profile.setEmail(email);
        for (String restriction : dietaryRestrictions) {
            profile.addDietaryRestriction(restriction);
        }
        for (Tag tag : tags) {
            profile.addTasteTag(tag);
        }
        return profile;
    }

    /**
     * Alias for toProfile so the UI can call a more explicit method.
     */
    public Profile createProfile() {
        return toProfile();
    }

    /**
     * Loads data from an existing profile back into the survey.
     *
     * @param profile the profile to copy from
     */
    public void loadFromProfile(Profile profile) {
        if (profile == null) {
            throw new IllegalArgumentException("Profile cannot be null");
        }
        this.name = profile.getName();
        this.email = profile.getEmail();
        this.age = profile.getAge();
        this.location = profile.getLocation();
        this.dietaryRestrictions.clear();
        this.dietaryRestrictions.addAll(profile.getDietaryRestrictions());
        this.tags.clear();
        this.tags.addAll(profile.getTasteProfile());
    }
}
