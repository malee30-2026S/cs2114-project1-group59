import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

/**
 * Handles all UI: the starting survey, the home page with recommendations,
 * search, the profile page, and the post-visit survey.
 */
public class Window
    extends JFrame
{

    private static String HOME_CARD = "HOME";
    private static String RESULTS_CARD = "RESULTS";
    private static String SURVEY_CARD = "SURVEY";
    private static String PROFILE_CARD = "PROFILE";

    /** Where profiles, ratings, and visits are saved between runs. */
    private static final String DATABASE_FILE = "hungryhokie.db";
    /** How many restaurants the "Recommended for you" section shows. */
    private static final int TOP_PICKS = 5;
    private static final String ANY_CUISINE = "Any cuisine";
    private static final String[] PRICE_CHOICES =
        {"Any price", "$", "$$", "$$$", "$$$$"};
    private static final String[] LOCATIONS =
        {"Blacksburg", "Christiansburg", "Roanoke", "Other"};
    private static final String NO_DIET = "None";
    private static final String[] DIETS =
        {NO_DIET, "Vegetarian", "Vegan", "Gluten-Free", "Dairy-Free"};
    private static final String[] CUISINES = {"Italian", "American", "Mexican",
        "Japanese", "Mediterranean", "Chinese", "Indian", "Vegan", "Thai", "Pizza", "BBQ"};
    private static final String DOT = "  \u00B7  ";

    private CardLayout cardLayout;
    private JPanel mainPanel;
    private HashMap<String, JPanel> cards;
    private Moderator moderator;
    private Profile currentProfile;
    private Survey currentSurvey;
    private Data yelpData;
    private ArrayList<Restaurant> localRestaurants;
    private Recommendations recommendationEngine;
    private DistanceCalculator distanceCalculator;
    private Database database;
    private boolean returningUser;
    private String flashMessage;
    private ArrayList<Restaurant> pendingVisits;

    // ----------------------------------------------------------
    /**
     * Create a new Window object.
     */
    public Window()
    {
        super("Hungry Hokie");
        this.moderator = new Moderator();
        this.currentProfile = new Profile();
        this.currentSurvey = new Survey();
        this.yelpData = Data.loadYelpAreaData();
        this.localRestaurants = new ArrayList<>(yelpData.getRestaurants());
        this.recommendationEngine = new Recommendations();
        this.distanceCalculator = new DistanceCalculator();
        this.database = openDatabase();
        this.pendingVisits = new ArrayList<>();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1040, 780);
        setMinimumSize(new Dimension(820, 600));
        setLocationRelativeTo(null);
        getContentPane().setBackground(Theme.BACKGROUND);
        addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                if (database != null)
                {
                    database.close();
                }
            }
        });

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        mainPanel.setBackground(Theme.BACKGROUND);
        cards = new HashMap<>();
        add(mainPanel);
    }


    /**
     * Opens the app. If someone was signed in last time, it goes straight to
     * their home page and asks how their last meals were. Otherwise it shows
     * the starting survey.
     */
    public void start()
    {
        Profile saved = loadLastUser();
        if (saved == null)
        {
            displaySurvey();
            return;
        }
        currentProfile = saved;
        returningUser = true;
        updateDistances();
        restoreReviews();
        displayHomeScreen();
        SwingUtilities.invokeLater(this::askAboutPendingVisits);
    }


    /**
     * Opens the database. The app still works without it; it just can't
     * remember anything after it closes.
     */
    private static Database openDatabase()
    {
        try
        {
            return new Database(DATABASE_FILE);
        }
        catch (IllegalStateException e)
        {
            String cause = e.getCause() == null ? "" : " (" + e.getCause().getMessage() + ")";
            System.err.println("Database unavailable, so nothing will be saved: "
                + e.getMessage() + cause);
            return null;
        }
    }


    private Profile loadLastUser()
    {
        if (database == null)
        {
            return null;
        }
        try
        {
            String email = database.getLastUser();
            return email == null ? null : database.loadProfile(email);
        }
        catch (IllegalStateException e)
        {
            warn("Couldn't load the last user", e);
            return null;
        }
    }


    // ---------------------------------------------------------------- Home

    /** Displays the home page */
    public void displayHomeScreen()
    {
        if (currentProfile == null || currentProfile.getName().isBlank())
        {
            displaySurvey();
            return;
        }
        refreshPendingVisits();

        JPanel content = pageContent();
        addFlashBanner(content);

        ArrayList<Restaurant> nearby = visibleRestaurants();
        ArrayList<Restaurant> ranked =
            recommendationEngine.rankRestaurants(currentProfile, nearby);
        ArrayList<Restaurant> topPicks =
            new ArrayList<>(ranked.subList(0, Math.min(TOP_PICKS, ranked.size())));

        content.add(Theme.sectionHeader("Recommended for you"));
        content.add(Theme.fullWidth(Theme.text(
            "Based on your survey, your ratings, and how close each place is.",
            Font.PLAIN, 13, Theme.GRAY)));
        content.add(Box.createVerticalStrut(10));
        addRestaurantCards(content, topPicks, true, this::displayHomeScreen);

        String where = coordinatesFor(currentProfile.getLocation()) == null
            ? "in the area"
            : "near " + currentProfile.getLocation() + ", closest first";
        content.add(Theme.sectionHeader("Everything " + where));
        addRestaurantCards(content, nearby, false, this::displayHomeScreen);

        String greeting = (returningUser ? "Welcome back, " : "Welcome, ")
            + currentProfile.getName() + DOT + currentProfile.getLocation();
        showScreen(
            HOME_CARD,
            Theme.headerBar("Hungry Hokie", greeting, searchNav(), profileNav()),
            Theme.scroll(content));
    }


    private void addRestaurantCards(
        JPanel content,
        ArrayList<Restaurant> restaurants,
        boolean numbered,
        Runnable refresh)
    {
        if (restaurants == null || restaurants.isEmpty())
        {
            content.add(messageCard("No restaurants found."));
            return;
        }
        for (int i = 0; i < restaurants.size(); i++)
        {
            content.add(createRestaurantCard(restaurants.get(i), numbered ? i + 1 : 0, refresh));
            content.add(Box.createVerticalStrut(10));
        }
    }


    /**
     * @param rank
     *            shown as a "#1" badge, or 0 for none
     * @param refresh
     *            redraws the current screen after the user eats at or hides
     *            the restaurant
     */
    private JComponent createRestaurantCard(Restaurant restaurant, int rank, Runnable refresh)
    {
        Theme.RoundedPanel card = Theme.card(new BorderLayout(16, 0));

        JPanel info = Theme.clear(new JPanel());
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));

        JPanel titleRow = row();
        if (rank > 0)
        {
            titleRow.add(Theme.chip("#" + rank, Theme.MAROON, Theme.WHITE));
        }
        titleRow.add(Theme.text(restaurant.getName(), Font.BOLD, 18, Theme.BLACK));
        if (currentProfile.getFavoriteRestaurants().contains(restaurant))
        {
            titleRow.add(Theme.chip("You'd go back", Theme.SUCCESS_TINT, Theme.SUCCESS));
        }
        if (pendingVisits.contains(restaurant))
        {
            titleRow.add(Theme.chip("We'll ask how it was next time", Theme.ORANGE_TINT, Theme.ORANGE_DARK));
        }
        info.add(Theme.fullWidth(titleRow));

        JPanel tagRow = row();
        for (Tag tag : restaurant.getTags())
        {
            tagRow.add(Theme.chip(tag.getName(), Theme.MAROON_TINT, Theme.MAROON));
        }
        info.add(Theme.fullWidth(tagRow));

        JPanel detailRow = row();
        detailRow.add(Theme.text(priceText(restaurant), Font.BOLD, 14, Theme.ORANGE_DARK));
        detailRow.add(Theme.text(distanceText(restaurant), Font.PLAIN, 14, Theme.GRAY_DARK));
        if (!restaurant.getAddress().isBlank())
        {
            detailRow.add(Theme.text(restaurant.getAddress(), Font.PLAIN, 14, Theme.GRAY));
        }
        String hours = yelpData.getHoursOpen(restaurant);
        if (!hours.isBlank())
        {
            detailRow.add(Theme.text(hours, Font.PLAIN, 14, Theme.GRAY));
        }
        info.add(Theme.fullWidth(detailRow));

        String bio = yelpData.getBio(restaurant);
        if (!bio.isBlank())
        {
            info.add(textRow(bio, Font.PLAIN, Theme.GRAY_DARK));
        }
        ArrayList<String> reviews = restaurant.getReviews();
        if (!reviews.isEmpty())
        {
            info.add(textRow("Your review: \"" + reviews.get(reviews.size() - 1) + "\"",
                Font.ITALIC, Theme.GRAY_DARK));
        }
        info.add(Box.createVerticalGlue());

        JButton eatButton = Theme.button("Eat here", Theme.ButtonStyle.PRIMARY);
        eatButton.setToolTipText("We'll ask how it was the next time you open the app");
        eatButton.addActionListener(e -> handleEatHere(restaurant, refresh));
        JButton hideButton = Theme.button("Hide", Theme.ButtonStyle.GHOST);
        hideButton.setToolTipText("Add to your blacklist so it never shows up again");
        hideButton.addActionListener(e -> handleHide(restaurant, refresh));

        JPanel buttons = Theme.clear(new JPanel(new GridLayout(2, 1, 0, 6)));
        buttons.add(eatButton);
        buttons.add(hideButton);
        JPanel buttonHolder = Theme.clear(new JPanel(new BorderLayout()));
        buttonHolder.add(buttons, BorderLayout.NORTH);

        card.add(info, BorderLayout.CENTER);
        card.add(buttonHolder, BorderLayout.EAST);
        return Theme.fullWidth(card);
    }


    /**
     * "Eat here" logs the visit. The post-visit survey pops up the next time
     * the app opens. Without a database there's nowhere to remember the
     * visit, so it asks right away instead.
     */
    private void handleEatHere(Restaurant restaurant, Runnable refresh)
    {
        if (database != null && !currentProfile.getEmail().isBlank())
        {
            try
            {
                database.addPendingVisit(currentProfile.getEmail(), restaurant);
                flashMessage = "Enjoy " + restaurant.getName()
                    + "! Next time you open Hungry Hokie, we'll ask how it was.";
                refresh.run();
                return;
            }
            catch (IllegalArgumentException | IllegalStateException e)
            {
                warn("Couldn't save the visit", e);
            }
        }
        ratePendingVisit(restaurant);
        refresh.run();
    }


    private void handleHide(Restaurant restaurant, Runnable refresh)
    {
        currentProfile.addToBlacklist(restaurant);
        saveProfile();
        flashMessage = "Hid " + restaurant.getName() + ". You can unhide it from My Profile.";
        refresh.run();
    }


    // ---------------------------------------------------------------- Post-visit survey

    /**
     * Shows the post-visit survey for every restaurant the user said they'd
     * eat at last time.
     */
    private void askAboutPendingVisits()
    {
        refreshPendingVisits();
        if (pendingVisits.isEmpty())
        {
            return;
        }
        int rated = 0;
        for (Restaurant restaurant : new ArrayList<>(pendingVisits))
        {
            if (ratePendingVisit(restaurant) == PostVisitDialog.Choice.SUBMITTED)
            {
                rated++;
            }
        }
        if (rated > 0)
        {
            flashMessage = "Thanks! Your " + (rated == 1 ? "rating" : rated + " ratings")
                + " updated your recommendations.";
        }
        displayHomeScreen();
    }


    /**
     * Shows the post-visit survey for one restaurant and applies the answer.
     */
    private PostVisitDialog.Choice ratePendingVisit(Restaurant restaurant)
    {
        PostVisitDialog dialog = new PostVisitDialog(this, restaurant, moderator);
        dialog.setVisible(true);
        PostVisitDialog.Choice choice = dialog.getChoice();
        if (choice == PostVisitDialog.Choice.SUBMITTED)
        {
            applyRating(restaurant, dialog.getStars(), dialog.getReview(), dialog.wouldGoBack());
        }
        if (choice != PostVisitDialog.Choice.LATER && database != null)
        {
            try
            {
                database.removePendingVisit(currentProfile.getEmail(), restaurant);
            }
            catch (IllegalStateException e)
            {
                warn("Couldn't clear the visit", e);
            }
        }
        return choice;
    }


    /**
     * The star rating raises or lowers the weight of every tag the
     * restaurant has, so recommendations learn from past visits.
     */
    private void applyRating(Restaurant restaurant, int stars, String review, boolean goBack)
    {
        double change = ratingToWeight(stars);
        for (Tag tag : restaurant.getTags())
        {
            currentProfile.addTagWeight(tag, change);
        }
        if (!review.isBlank() && !restaurant.getReviews().contains(review))
        {
            restaurant.addReview(review);
        }
        if (goBack)
        {
            currentProfile.addFavoriteRestaurant(restaurant);
        }
        if (database != null && !currentProfile.getEmail().isBlank())
        {
            try
            {
                database.saveRating(currentProfile.getEmail(), restaurant, stars, review);
            }
            catch (IllegalArgumentException | IllegalStateException e)
            {
                warn("Couldn't save the rating", e);
            }
        }
        saveProfile();
    }


    /**
     * 5 stars = +1, 3 stars = no change, 1 star = -1 for each of the
     * restaurant's tags.
     */
    private static double ratingToWeight(int stars)
    {
        return (stars - 3) / 2.0;
    }


    // ---------------------------------------------------------------- Search

    /**
     * Displays the search screen, starting with a list of restaurants
     *
     * @param results
     *            an array list of restaurants
     */
    public void displaySearchResults(ArrayList<Restaurant> results)
    {
        refreshPendingVisits();

        JTextField nameField = Theme.styleField(new JTextField(12));
        JComboBox<String> cuisineCombo = Theme.styleCombo(new JComboBox<>(cuisineChoices()));
        JComboBox<String> priceCombo = Theme.styleCombo(new JComboBox<>(PRICE_CHOICES));
        JTextField milesField = Theme.styleField(new JTextField(6));

        Theme.RoundedPanel filters = Theme.card(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(3, 6, 3, 6);
        String[] labels = {"Name contains", "Cuisine", "Max price", "Max distance (miles)"};
        JComponent[] inputs = {nameField, cuisineCombo, priceCombo, milesField};
        for (int i = 0; i < inputs.length; i++)
        {
            c.gridx = i;
            c.gridy = 0;
            filters.add(Theme.text(labels[i], Font.BOLD, 13, Theme.GRAY_DARK), c);
            c.gridy = 1;
            filters.add(inputs[i], c);
        }

        JLabel errorLabel = Theme.text(" ", Font.BOLD, 13, Theme.ERROR);
        JButton searchButton = Theme.button("Search", Theme.ButtonStyle.PRIMARY);
        JButton clearButton = Theme.button("Clear", Theme.ButtonStyle.GHOST);
        JPanel actions = Theme.clear(new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0)));
        actions.add(searchButton);
        actions.add(clearButton);
        actions.add(Box.createHorizontalStrut(8));
        actions.add(errorLabel);
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = inputs.length;
        c.insets = new Insets(12, 0, 0, 0);
        filters.add(actions, c);

        JPanel resultsHolder = Theme.clear(new JPanel(new BorderLayout()));
        Runnable showAll = () -> displaySearchResults(visibleRestaurants());
        resultsHolder.add(Theme.scroll(createResultsView(results, showAll)), BorderLayout.CENTER);

        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setBackground(Theme.BACKGROUND);
        body.setBorder(new EmptyBorder(18, 24, 0, 24));
        body.add(filters, BorderLayout.NORTH);
        body.add(resultsHolder, BorderLayout.CENTER);

        Runnable search = () -> runSearch(
            nameField, cuisineCombo, priceCombo, milesField, errorLabel, resultsHolder);
        searchButton.addActionListener(e -> search.run());
        nameField.addActionListener(e -> search.run());
        milesField.addActionListener(e -> search.run());
        clearButton.addActionListener(e -> showAll.run());

        showScreen(
            RESULTS_CARD,
            Theme.headerBar("Search", "Filter by name, cuisine, price, and distance", homeNav(), profileNav()),
            body);
    }


    /**
     * Runs the search with whatever filters are filled in. Bad input shows a
     * red message instead of crashing.
     */
    private void runSearch(
        JTextField nameField,
        JComboBox<String> cuisineCombo,
        JComboBox<String> priceCombo,
        JTextField milesField,
        JLabel errorLabel,
        JPanel resultsHolder)
    {
        ArrayList<Restaurant> results;
        try
        {
            results = filterRestaurants(
                nameField.getText(),
                (String)cuisineCombo.getSelectedItem(),
                priceCombo.getSelectedIndex(),
                milesField.getText());
        }
        catch (IllegalArgumentException e)
        {
            errorLabel.setText(e.getMessage());
            return;
        }
        errorLabel.setText(" ");
        refreshPendingVisits();

        Runnable refresh = () -> runSearch(
            nameField, cuisineCombo, priceCombo, milesField, errorLabel, resultsHolder);
        resultsHolder.removeAll();
        resultsHolder.add(Theme.scroll(createResultsView(results, refresh)), BorderLayout.CENTER);
        resultsHolder.revalidate();
        resultsHolder.repaint();
    }


    /**
     * Applies each filter the user filled in, one Search at a time.
     *
     * @throws IllegalArgumentException
     *             with a message for the user if any filter is invalid
     */
    private ArrayList<Restaurant> filterRestaurants(
        String name,
        String cuisine,
        int maxPrice,
        String milesText)
    {
        ArrayList<Restaurant> results = visibleRestaurants();
        if (!name.isBlank())
        {
            if (!moderator.validateInput(name))
            {
                throw new IllegalArgumentException(
                    "Restaurant names need at least one letter or number.");
            }
            results = new Search(results).searchByName(name);
        }
        if (cuisine != null && !cuisine.equals(ANY_CUISINE))
        {
            results = new Search(results).searchByTag(new Tag(cuisine));
        }
        if (maxPrice > 0)
        {
            results = new Search(results).searchByPrice(maxPrice);
        }
        if (!milesText.isBlank())
        {
            double miles;
            try
            {
                miles = Double.parseDouble(milesText.trim());
            }
            catch (NumberFormatException e)
            {
                throw new IllegalArgumentException(
                    "Max distance must be a number of miles, like 2 or 0.5.");
            }
            results = new Search(results).searchByDistance(miles);
        }
        return results;
    }


    private JPanel createResultsView(ArrayList<Restaurant> results, Runnable refresh)
    {
        JPanel view = Theme.verticalList();
        view.setBorder(new EmptyBorder(0, 0, 24, 0));
        addFlashBanner(view);
        int count = results == null ? 0 : results.size();
        view.add(Theme.fullWidth(Theme.text(
            count + " restaurant" + (count == 1 ? "" : "s") + " found",
            Font.BOLD, 14, Theme.GRAY_DARK)));
        view.add(Box.createVerticalStrut(8));
        addRestaurantCards(view, results, false, refresh);
        return view;
    }


    private String[] cuisineChoices()
    {
        ArrayList<String> choices = new ArrayList<>();
        for (Tag tag : yelpData.getTags())
        {
            choices.add(tag.getName());
        }
        choices.sort(String.CASE_INSENSITIVE_ORDER);
        choices.add(0, ANY_CUISINE);
        return choices.toArray(new String[0]);
    }


    // ---------------------------------------------------------------- Profile

    /**
     * Displays everything the app has saved about the user.
     */
    public void displayProfile()
    {
        refreshPendingVisits();
        JPanel content = pageContent();
        addFlashBanner(content);

        content.add(Theme.sectionHeader("About you"));
        Theme.RoundedPanel about = Theme.card(new GridBagLayout());
        int row = 0;
        addInfoRow(about, row++, "Name", Theme.text(currentProfile.getName(), Font.PLAIN, 15, Theme.BLACK));
        addInfoRow(about, row++, "Email", Theme.text(currentProfile.getEmail(), Font.PLAIN, 15, Theme.BLACK));
        addInfoRow(about, row++, "Age", Theme.text(
            currentProfile.getAge() == 0 ? "Not set" : String.valueOf(currentProfile.getAge()),
            Font.PLAIN, 15, Theme.BLACK));
        addInfoRow(about, row++, "Location", Theme.text(currentProfile.getLocation(), Font.PLAIN, 15, Theme.BLACK));
        String diets = currentProfile.getDietaryRestrictions().isEmpty()
            ? "None"
            : String.join(", ", currentProfile.getDietaryRestrictions());
        addInfoRow(about, row++, "Dietary restriction", Theme.text(diets, Font.PLAIN, 15, Theme.BLACK));
        JPanel likes = Theme.clear(new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0)));
        for (Tag tag : currentProfile.getTasteProfile())
        {
            likes.add(Theme.chip(tag.getName(), Theme.MAROON_TINT, Theme.MAROON));
        }
        for (String cuisine : currentProfile.getFavoriteCuisines())
        {
            likes.add(Theme.chip(cuisine, Theme.MAROON_TINT, Theme.MAROON));
        }
        addInfoRow(about, row++, "Favorite cuisine", likes);
        String storage = database == null
            ? "Not saved (the database couldn't be opened)"
            : "Saved on this computer in " + DATABASE_FILE;
        addInfoRow(about, row++, "Stored", Theme.text(storage, Font.PLAIN, 13, Theme.GRAY));
        content.add(Theme.fullWidth(about));
        content.add(Box.createVerticalStrut(10));

        JButton editButton = Theme.button("Edit profile", Theme.ButtonStyle.SECONDARY);
        editButton.addActionListener(e -> displaySurvey());
        JButton signOutButton = Theme.button("Sign out", Theme.ButtonStyle.GHOST);
        signOutButton.addActionListener(e -> signOut());
        JPanel accountButtons = Theme.clear(new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)));
        accountButtons.add(editButton);
        accountButtons.add(Box.createHorizontalStrut(10));
        accountButtons.add(signOutButton);
        content.add(Theme.fullWidth(accountButtons));

        content.add(Theme.sectionHeader("What Hungry Hokie has learned from your ratings"));
        content.add(learnedCard());

        if (!pendingVisits.isEmpty())
        {
            content.add(Theme.sectionHeader("Waiting for your rating"));
            JPanel pendingList = listPanel();
            for (Restaurant restaurant : pendingVisits)
            {
                JButton rateButton = Theme.button("Rate now", Theme.ButtonStyle.SECONDARY);
                rateButton.addActionListener(e -> {
                    if (ratePendingVisit(restaurant) == PostVisitDialog.Choice.SUBMITTED)
                    {
                        flashMessage = "Thanks! Your rating updated your recommendations.";
                    }
                    displayProfile();
                });
                addListRow(pendingList, restaurant.getName(),
                    "We'll ask how it was the next time you open the app", rateButton);
            }
            content.add(cardAround(pendingList));
        }

        content.add(Theme.sectionHeader("Your ratings"));
        content.add(ratingsCard());

        content.add(Theme.sectionHeader("Places you'd go back to"));
        ArrayList<Restaurant> favorites = currentProfile.getFavoriteRestaurants();
        if (favorites.isEmpty())
        {
            content.add(messageCard("None yet. Check \"I'd go back here\" when you rate a place."));
        }
        else
        {
            JPanel favoriteList = listPanel();
            for (Restaurant restaurant : favorites)
            {
                addListRow(favoriteList, restaurant.getName(), resolve(restaurant).getAddress(), null);
            }
            content.add(cardAround(favoriteList));
        }

        content.add(Theme.sectionHeader("Hidden restaurants"));
        ArrayList<Restaurant> hidden = currentProfile.getBlacklist().getBlacklistedRestaurants();
        if (hidden.isEmpty())
        {
            content.add(messageCard("Nothing hidden. Use \"Hide\" on a restaurant you never want to see."));
        }
        else
        {
            JPanel hiddenList = listPanel();
            for (Restaurant restaurant : hidden)
            {
                JButton unhideButton = Theme.button("Unhide", Theme.ButtonStyle.SECONDARY);
                unhideButton.addActionListener(e -> {
                    currentProfile.getBlacklist().removeRestaurant(restaurant);
                    saveProfile();
                    flashMessage = restaurant.getName() + " will show up again.";
                    displayProfile();
                });
                addListRow(hiddenList, restaurant.getName(), resolve(restaurant).getAddress(), unhideButton);
            }
            content.add(cardAround(hiddenList));
        }

        showScreen(
            PROFILE_CARD,
            Theme.headerBar("My Profile", "Everything Hungry Hokie has saved about you",
                homeNav(), searchNav()),
            Theme.scroll(content));
    }


    /** Tag weights from past ratings, most liked first. */
    private JComponent learnedCard()
    {
        HashMap<Tag, Double> weights = currentProfile.getTagWeights();
        weights.values().removeIf(weight -> weight == 0);
        if (weights.isEmpty())
        {
            return messageCard("Nothing yet. Tap \"Eat here\" on a restaurant, and the next time you "
                + "open the app we'll ask how it was.");
        }
        ArrayList<Map.Entry<Tag, Double>> sorted = new ArrayList<>(weights.entrySet());
        sorted.sort(Map.Entry.<Tag, Double>comparingByValue().reversed());
        double largest = 0;
        for (Map.Entry<Tag, Double> entry : sorted)
        {
            largest = Math.max(largest, Math.abs(entry.getValue()));
        }

        Theme.RoundedPanel card = Theme.card(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(5, 0, 5, 16);
        int row = 0;
        for (Map.Entry<Tag, Double> entry : sorted)
        {
            double weight = entry.getValue();
            c.gridy = row++;
            c.gridx = 0;
            c.weightx = 0;
            card.add(Theme.chip(entry.getKey().getName(), Theme.MAROON_TINT, Theme.MAROON), c);
            c.gridx = 1;
            card.add(new Theme.WeightBar(weight, largest), c);
            c.gridx = 2;
            c.weightx = 1;
            String meaning = weight > 0 ? "ranks higher for you" : "ranks lower for you";
            card.add(Theme.text(String.format(Locale.ROOT, "%+.1f  %s", weight, meaning),
                Font.PLAIN, 14, weight > 0 ? Theme.ORANGE_DARK : Theme.GRAY), c);
        }
        return Theme.fullWidth(card);
    }


    private JComponent ratingsCard()
    {
        if (database == null)
        {
            return messageCard("Ratings are only kept while the app is open, because the database "
                + "couldn't be opened.");
        }
        ArrayList<Rating> ratings;
        try
        {
            ratings = database.getRatings(currentProfile.getEmail());
        }
        catch (IllegalStateException e)
        {
            warn("Couldn't load ratings", e);
            ratings = new ArrayList<>();
        }
        if (ratings.isEmpty())
        {
            return messageCard("No ratings yet.");
        }
        JPanel list = listPanel();
        for (Rating rating : ratings)
        {
            JPanel entry = Theme.clear(new JPanel());
            entry.setLayout(new BoxLayout(entry, BoxLayout.Y_AXIS));
            JPanel top = row();
            top.add(Theme.text(rating.getRestaurantName(), Font.BOLD, 15, Theme.BLACK));
            top.add(new Theme.StarRating(rating.getStars(), false, 16));
            top.add(Theme.text(rating.getDate(), Font.PLAIN, 13, Theme.GRAY));
            entry.add(Theme.fullWidth(top));
            if (!rating.getReview().isEmpty())
            {
                entry.add(textRow("\"" + rating.getReview() + "\"", Font.ITALIC, Theme.GRAY_DARK));
            }
            addListRow(list, entry);
        }
        return cardAround(list);
    }


    private static void addInfoRow(JPanel card, int row, String label, JComponent value)
    {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(5, 0, 5, 24);
        c.gridx = 0;
        card.add(Theme.text(label, Font.BOLD, 13, Theme.GRAY), c);
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        card.add(value, c);
    }


    private void signOut()
    {
        if (database != null)
        {
            try
            {
                database.clearLastUser();
            }
            catch (IllegalStateException e)
            {
                warn("Couldn't sign out", e);
            }
        }
        // Fresh restaurant objects, so the next user doesn't see these reviews
        yelpData = Data.loadYelpAreaData();
        localRestaurants = new ArrayList<>(yelpData.getRestaurants());
        currentProfile = new Profile();
        returningUser = false;
        flashMessage = null;
        displaySurvey();
    }


    // ---------------------------------------------------------------- Survey

    /** Displays the survey. */
    public void displaySurvey()
    {
        currentSurvey = new Survey();
        boolean editing = !currentProfile.getName().isBlank();

        JTextField nameField = Theme.styleField(new JTextField(currentProfile.getName(), 28));
        JTextField emailField = Theme.styleField(new JTextField(currentProfile.getEmail(), 28));
        JComboBox<Integer> ageCombo = new JComboBox<>();
        for (int i = 18; i <= 80; i++)
        {
            ageCombo.addItem(i);
        }
        Theme.styleCombo(ageCombo);
        JComboBox<String> locationCombo = Theme.styleCombo(new JComboBox<>(LOCATIONS));
        JComboBox<String> dietaryCombo = Theme.styleCombo(new JComboBox<>(DIETS));
        JComboBox<String> cuisineCombo = Theme.styleCombo(new JComboBox<>(CUISINES));
        if (editing)
        {
            ageCombo.setSelectedItem(currentProfile.getAge());
            locationCombo.setSelectedItem(currentProfile.getLocation());
            if (!currentProfile.getDietaryRestrictions().isEmpty())
            {
                dietaryCombo.setSelectedItem(currentProfile.getDietaryRestrictions().get(0));
            }
            if (!currentProfile.getTasteProfile().isEmpty())
            {
                cuisineCombo.setSelectedItem(currentProfile.getTasteProfile().get(0).getName());
            }
        }

        Theme.RoundedPanel form = Theme.card(new GridBagLayout());
        form.setBorder(new EmptyBorder(24, 28, 24, 28));
        GridBagConstraints bottom = new GridBagConstraints();
        bottom.gridx = 0;
        bottom.gridwidth = 2;
        bottom.fill = GridBagConstraints.HORIZONTAL;
        bottom.weightx = 1;
        bottom.gridy = 0;
        bottom.insets = new Insets(0, 0, 4, 0);
        form.add(Theme.text(editing ? "Update your profile" : "Tell us about you",
            Font.BOLD, 22, Theme.MAROON), bottom);
        bottom.gridy = 1;
        bottom.insets = new Insets(0, 0, 18, 0);
        form.add(Theme.text("We use this to pick restaurants you'll like.",
            Font.PLAIN, 14, Theme.GRAY), bottom);

        int row = addFormField(form, 2, 0, 2, "Name", nameField);
        row = addFormField(form, row, 0, 2, "Email (so we remember your ratings next time)", emailField);
        addFormField(form, row, 0, 1, "Age", ageCombo);
        row = addFormField(form, row, 1, 1, "Location", locationCombo);
        addFormField(form, row, 0, 1, "Dietary restriction", dietaryCombo);
        row = addFormField(form, row, 1, 1, "Favorite cuisine", cuisineCombo);

        bottom.gridy = row;
        bottom.insets = new Insets(0, 0, 0, 0);
        JLabel errorLabel = Theme.text(" ", Font.BOLD, 13, Theme.ERROR);
        form.add(errorLabel, bottom);

        JButton submitButton = Theme.button(editing ? "Save changes" : "Find my food",
            Theme.ButtonStyle.PRIMARY);
        JPanel buttons = Theme.clear(new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)));
        buttons.add(submitButton);
        if (editing)
        {
            JButton cancelButton = Theme.button("Cancel", Theme.ButtonStyle.GHOST);
            cancelButton.addActionListener(e -> displayProfile());
            buttons.add(Box.createHorizontalStrut(10));
            buttons.add(cancelButton);
        }
        bottom.gridy++;
        bottom.insets = new Insets(10, 0, 0, 0);
        form.add(buttons, bottom);
        bottom.gridy++;
        form.add(Box.createHorizontalStrut(480), bottom);

        submitButton.addActionListener(e -> {
            String name = nameField.getText();
            String email = emailField.getText();
            String location = (String)locationCombo.getSelectedItem();
            Object ageValue = ageCombo.getSelectedItem();

            if (!moderator.validateInput(name))
            {
                errorLabel.setText(
                    "Please enter a valid name (it needs at least one letter or number).");
                return;
            }
            if (!moderator.validateEmail(email))
            {
                errorLabel.setText("Please enter a valid email, like pid@vt.edu.");
                return;
            }
            if (location == null || location.trim().isEmpty())
            {
                errorLabel.setText("Please select a location.");
                return;
            }
            if (ageValue == null)
            {
                errorLabel.setText("Please select an age.");
                return;
            }

            currentSurvey = new Survey();
            currentSurvey.setName(name.trim());
            currentSurvey.setEmail(email.trim());
            currentSurvey.setAge((Integer)ageValue);
            currentSurvey.setLocation(location.trim());

            String dietaryChoice = (String)dietaryCombo.getSelectedItem();
            if (dietaryChoice != null && !dietaryChoice.equals(NO_DIET))
            {
                currentSurvey.addDietaryRestriction(dietaryChoice);
            }

            String cuisineChoice = (String)cuisineCombo.getSelectedItem();
            if (cuisineChoice != null && !cuisineChoice.trim().isEmpty())
            {
                currentSurvey.addTag(cuisineChoice.trim());
            }

            Profile newProfile = currentSurvey.toProfile();
            mergeSavedData(newProfile);
            currentProfile = newProfile;
            saveProfile();
            rememberSignedIn();
            updateDistances();
            restoreReviews();
            flashMessage = editing ? "Profile saved." : null;
            displayHomeScreen();
            if (!editing)
            {
                SwingUtilities.invokeLater(this::askAboutPendingVisits);
            }
        });

        JPanel centered = new JPanel(new GridBagLayout());
        centered.setBackground(Theme.BACKGROUND);
        centered.setBorder(new EmptyBorder(28, 24, 28, 24));
        GridBagConstraints middle = new GridBagConstraints();
        middle.anchor = GridBagConstraints.NORTH;
        middle.weighty = 1;
        centered.add(form, middle);

        showScreen(
            SURVEY_CARD,
            Theme.headerBar("Hungry Hokie",
                "Find your next favorite place to eat in Blacksburg and Christiansburg"),
            Theme.scroll(centered));
        nameField.requestFocusInWindow();
    }


    /**
     * Adds a label with its input underneath.
     *
     * @param column
     *            0 or 1
     * @param width
     *            2 to span both columns
     * @return the next free row
     */
    private static int addFormField(
        JPanel form,
        int row,
        int column,
        int width,
        String label,
        JComponent input)
    {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = column;
        c.gridy = row;
        c.gridwidth = width;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        int leftGap = column == 1 ? 8 : 0;
        int rightGap = column == 0 && width == 1 ? 8 : 0;
        c.insets = new Insets(0, leftGap, 4, rightGap);
        form.add(Theme.text(label, Font.BOLD, 13, Theme.GRAY_DARK), c);
        c.gridy = row + 1;
        c.insets = new Insets(0, leftGap, 16, rightGap);
        form.add(input, c);
        return row + 2;
    }


    /**
     * Carries over what the app already knows about this email: learned
     * preferences, favorites, and hidden restaurants.
     */
    private void mergeSavedData(Profile newProfile)
    {
        Profile saved = null;
        returningUser = false;
        if (database != null)
        {
            try
            {
                saved = database.loadProfile(newProfile.getEmail());
                returningUser = saved != null;
            }
            catch (IllegalStateException e)
            {
                warn("Couldn't load saved preferences", e);
            }
        }
        if (saved == null && !currentProfile.getEmail().isBlank()
            && currentProfile.getEmail().equalsIgnoreCase(newProfile.getEmail()))
        {
            saved = currentProfile;
        }
        if (saved == null)
        {
            return;
        }
        saved.getTagWeights().forEach(newProfile::addTagWeight);
        for (String cuisine : saved.getFavoriteCuisines())
        {
            newProfile.addFavoriteCuisine(cuisine);
        }
        for (Restaurant r : saved.getFavoriteRestaurants())
        {
            newProfile.addFavoriteRestaurant(r);
        }
        for (Restaurant r : saved.getBlacklist().getBlacklistedRestaurants())
        {
            newProfile.addToBlacklist(r);
        }
        for (Tag tag : saved.getBlacklist().getBlacklistedTags())
        {
            newProfile.addToBlacklistTag(tag);
        }
    }


    // ---------------------------------------------------------------- Saving

    private void saveProfile()
    {
        if (database == null || currentProfile.getEmail().isBlank())
        {
            return;
        }
        try
        {
            database.saveProfile(currentProfile);
        }
        catch (IllegalArgumentException | IllegalStateException e)
        {
            warn("Couldn't save the profile", e);
        }
    }


    private void rememberSignedIn()
    {
        if (database == null || currentProfile.getEmail().isBlank())
        {
            return;
        }
        try
        {
            database.setLastUser(currentProfile.getEmail());
        }
        catch (IllegalArgumentException | IllegalStateException e)
        {
            warn("Couldn't remember who's signed in", e);
        }
    }


    private void refreshPendingVisits()
    {
        pendingVisits = new ArrayList<>();
        if (database == null || currentProfile.getEmail().isBlank())
        {
            return;
        }
        try
        {
            for (Restaurant r : database.getPendingVisits(currentProfile.getEmail()))
            {
                pendingVisits.add(resolve(r));
            }
        }
        catch (IllegalStateException e)
        {
            warn("Couldn't load visits", e);
        }
    }


    /** Puts saved reviews back on the restaurants so their cards show them. */
    private void restoreReviews()
    {
        if (database == null || currentProfile.getEmail().isBlank())
        {
            return;
        }
        try
        {
            ArrayList<Rating> ratings = database.getRatings(currentProfile.getEmail());
            for (int i = ratings.size() - 1; i >= 0; i--)
            {
                Rating rating = ratings.get(i);
                Restaurant restaurant = findLocal(new Restaurant(rating.getRestaurantName(),
                    new ArrayList<>(), Restaurant.PRICE_UNKNOWN, Double.NaN, Double.NaN,
                    rating.getRestaurantAddress()));
                if (restaurant != null && !rating.getReview().isEmpty()
                    && !restaurant.getReviews().contains(rating.getReview()))
                {
                    restaurant.addReview(rating.getReview());
                }
            }
        }
        catch (IllegalStateException e)
        {
            warn("Couldn't load reviews", e);
        }
    }


    private static void warn(String what, RuntimeException e)
    {
        System.err.println(what + ": " + e.getMessage());
    }


    // ---------------------------------------------------------------- Helpers

    /**
     * Measures every restaurant's distance from the user's town, or marks it
     * unknown if we don't have coordinates for their location.
     */
    private void updateDistances()
    {
        double[] here = coordinatesFor(currentProfile.getLocation());
        for (Restaurant restaurant : localRestaurants)
        {
            if (here == null || !restaurant.hasLocation())
            {
                restaurant.clearDistance();
            }
            else
            {
                distanceCalculator.getDistance(here[0], here[1], restaurant);
            }
        }
    }


    /**
     * @return {latitude, longitude} of the survey's location choices, or null
     *         for "Other"
     */
    private static double[] coordinatesFor(String location)
    {
        switch (location == null ? "" : location.toLowerCase(Locale.ROOT))
        {
            case "blacksburg":
                return new double[] {37.2296, -80.4139}; // Virginia Tech
            case "christiansburg":
                return new double[] {37.1299, -80.4089}; // downtown
            case "roanoke":
                return new double[] {37.2710, -79.9414}; // downtown
            default:
                return null;
        }
    }


    /** Every restaurant not on the user's blacklist, closest first. */
    private ArrayList<Restaurant> visibleRestaurants()
    {
        ArrayList<Restaurant> visible = new ArrayList<>();
        for (Restaurant restaurant : localRestaurants)
        {
            if (!currentProfile.isBlacklisted(restaurant))
            {
                visible.add(restaurant);
            }
        }
        visible.sort(Comparator
            .comparingDouble((Restaurant r) -> Double.isNaN(r.getDistance())
                ? Double.MAX_VALUE
                : r.getDistance())
            .thenComparing(r -> r.getName().toLowerCase(Locale.ROOT)));
        return visible;
    }


    /** The app's full copy of a restaurant, or null if it isn't in the data. */
    private Restaurant findLocal(Restaurant key)
    {
        for (Restaurant restaurant : localRestaurants)
        {
            if (restaurant.equals(key))
            {
                return restaurant;
            }
        }
        return null;
    }


    /** The app's full copy of a restaurant, or the one given if there isn't one. */
    private Restaurant resolve(Restaurant key)
    {
        Restaurant local = findLocal(key);
        return local == null ? key : local;
    }


    private static String priceText(Restaurant restaurant)
    {
        return restaurant.getPriceLevel() == Restaurant.PRICE_UNKNOWN
            ? "Price unknown"
            : restaurant.getPriceSymbol();
    }


    private static String distanceText(Restaurant restaurant)
    {
        double miles = restaurant.getDistance();
        if (Double.isNaN(miles))
        {
            return "Distance unknown";
        }
        if (miles < 0.05)
        {
            return "under 0.1 mi away";
        }
        return String.format(Locale.ROOT, "%.1f mi away", miles);
    }


    private JButton homeNav()
    {
        JButton button = Theme.button("Home", Theme.ButtonStyle.HEADER);
        button.addActionListener(e -> displayHomeScreen());
        return button;
    }


    private JButton searchNav()
    {
        JButton button = Theme.button("Search", Theme.ButtonStyle.HEADER);
        button.addActionListener(e -> displaySearchResults(visibleRestaurants()));
        return button;
    }


    private JButton profileNav()
    {
        JButton button = Theme.button("My Profile", Theme.ButtonStyle.HEADER);
        button.addActionListener(e -> displayProfile());
        return button;
    }


    private void addFlashBanner(JPanel content)
    {
        if (flashMessage != null)
        {
            content.add(Box.createVerticalStrut(12));
            content.add(Theme.banner(flashMessage));
            flashMessage = null;
        }
    }


    private static JPanel pageContent()
    {
        JPanel content = Theme.verticalList();
        content.setBorder(new EmptyBorder(6, 24, 28, 24));
        return content;
    }


    private static JPanel row()
    {
        return Theme.clear(new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 3)));
    }


    private static JComponent textRow(String text, int style, java.awt.Color color)
    {
        JPanel row = row();
        row.add(Theme.text(text, style, 14, color));
        return Theme.fullWidth(row);
    }


    private static JComponent messageCard(String message)
    {
        Theme.RoundedPanel card = Theme.card(new BorderLayout());
        card.add(Theme.text(message, Font.PLAIN, 14, Theme.GRAY), BorderLayout.CENTER);
        return Theme.fullWidth(card);
    }


    private static JPanel listPanel()
    {
        JPanel list = Theme.clear(new JPanel());
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        return list;
    }


    private static void addListRow(JPanel list, String title, String subtitle, JButton action)
    {
        JPanel text = Theme.clear(new JPanel());
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(Theme.text(title, Font.BOLD, 15, Theme.BLACK));
        if (subtitle != null && !subtitle.isBlank())
        {
            text.add(Theme.text(subtitle, Font.PLAIN, 13, Theme.GRAY));
        }
        JPanel entry = Theme.clear(new JPanel(new BorderLayout(12, 0)));
        entry.add(text, BorderLayout.CENTER);
        if (action != null)
        {
            JPanel holder = Theme.clear(new JPanel(new GridBagLayout()));
            holder.add(action);
            entry.add(holder, BorderLayout.EAST);
        }
        addListRow(list, entry);
    }


    private static void addListRow(JPanel list, JComponent entry)
    {
        if (list.getComponentCount() > 0)
        {
            entry.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.GRAY_LIGHT),
                new EmptyBorder(10, 0, 10, 0)));
        }
        else
        {
            entry.setBorder(new EmptyBorder(0, 0, 10, 0));
        }
        list.add(Theme.fullWidth(entry));
    }


    private static JComponent cardAround(JPanel list)
    {
        Theme.RoundedPanel card = Theme.card(new BorderLayout());
        card.add(list, BorderLayout.CENTER);
        return Theme.fullWidth(card);
    }


    private void showScreen(String name, JComponent header, JComponent body)
    {
        JPanel screen = new JPanel(new BorderLayout());
        screen.setBackground(Theme.BACKGROUND);
        screen.add(header, BorderLayout.NORTH);
        screen.add(body, BorderLayout.CENTER);
        showCard(name, screen);
    }


    /**
     * Shows a screen, replacing the old copy of it so panels don't pile up.
     */
    private void showCard(String name, JPanel panel)
    {
        JPanel old = cards.put(name, panel);
        if (old != null)
        {
            mainPanel.remove(old);
        }
        mainPanel.add(panel, name);
        cardLayout.show(mainPanel, name);
        mainPanel.revalidate();
        mainPanel.repaint();
        setVisible(true);
    }


    // ----------------------------------------------------------
    /**
     * main method
     *
     * @param args
     *            command line arguments
     */
    public static void main(String[] args)
    {
        Theme.install();
        SwingUtilities.invokeLater(() -> new Window().start());
    }
}
