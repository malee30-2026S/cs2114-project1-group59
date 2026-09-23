import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Handles all UI: the starting survey, the home page with recommendations,
 * search, the profile page, and the post-visit survey page.
 */
public class Window
    extends JFrame
{

    private static String HOME_CARD = "HOME";
    private static String RESULTS_CARD = "RESULTS";
    private static String SURVEY_CARD = "SURVEY";
    private static String PROFILE_CARD = "PROFILE";
    private static String POST_VISIT_CARD = "POST_VISIT";

    /** Where profiles, ratings, visits, and photos are saved between runs. */
    private static final String DATABASE_FILE = "hungryhokie.db";
    /** How many restaurants the "Recommended for you" section shows. */
    private static final int TOP_PICKS = 5;
    /** Profile photos are cropped square and shrunk to this many pixels. */
    private static final int PHOTO_SIZE = 256;
    private static final long MAX_PHOTO_BYTES = 15L * 1024 * 1024;
    private static final String ANY_CUISINE = "Any cuisine";
    private static final String[] PRICE_CHOICES =
        {"Any price", "$", "$$", "$$$", "$$$$"};
    private static final String CURRENT_LOCATION = "Current location";
    private static final String[] LOCATIONS =
        {CURRENT_LOCATION, "Blacksburg", "Christiansburg", "Roanoke", "Other"};
    /** Towns you can pick instead of live location, and where distances are measured from. */
    private static final String[] TOWN_NAMES = {"Blacksburg", "Christiansburg", "Roanoke"};
    private static final double[][] TOWN_COORDINATES =
        {{37.2296, -80.4139}, {37.1299, -80.4089}, {37.2710, -79.9414}};
    private static final String NO_DIET = "None";
    private static final String[] DIETS =
        {NO_DIET, "Vegetarian", "Vegan", "Gluten-Free", "Dairy-Free"};
    private static final String[] CUISINES = {"Italian", "American", "Mexican",
        "Japanese", "Mediterranean", "Chinese", "Indian", "Vegan", "Thai", "Pizza", "BBQ"};
    private static final String DISH_EXAMPLES = "tacos, sushi, pizza, curry, pad thai, or gyros";
    private static final String DOT = "  \u00B7  ";

    private CardLayout cardLayout;
    private JPanel mainPanel;
    private HashMap<String, JPanel> cards;
    private String currentCard;
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
    private boolean flashIsError;
    private String searchNote;
    private ArrayList<Restaurant> pendingVisits;
    // Only used when the database can't be opened
    private byte[] sessionPhoto;
    private Boolean sessionLocationPermission;

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
        setSize(1060, 800);
        setMinimumSize(new Dimension(860, 620));
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
     * Opens the app. The starting survey only shows the first time; after
     * that the app remembers who's signed in. If they clicked "Eat here"
     * last time, the post-visit survey page comes up before the home page.
     */
    public void start()
    {
        Profile saved = loadLastUser();
        if (saved == null)
        {
            displaySurvey();
            return;
        }
        returningUser = true;
        openFor(saved, true);
    }


    /**
     * Makes a profile the current user and shows their first screen: the
     * post-visit survey if they have visits to rate, otherwise home.
     */
    private void openFor(Profile profile, boolean refreshLocation)
    {
        currentProfile = profile;
        updateDistances();
        restoreReviews();
        if (refreshLocation)
        {
            refreshLiveLocation();
        }
        if (!showPostVisitSurveys())
        {
            displayHomeScreen();
        }
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
        content.add(Box.createVerticalStrut(14));
        content.add(cravingCard());

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

        String near = CURRENT_LOCATION.equals(currentProfile.getLocation())
            ? "you"
            : currentProfile.getLocation();
        String where = userCoordinates() == null
            ? "in the area"
            : "near " + near + ", closest first";
        content.add(Theme.sectionHeader("Everything " + where));
        addRestaurantCards(content, nearby, false, this::displayHomeScreen);

        String greeting = (returningUser ? "Welcome back, " : "Welcome, ")
            + currentProfile.getName() + DOT + displayLocation();
        showScreen(
            HOME_CARD,
            Theme.headerBar("Hungry Hokie", greeting, searchNav(), profileNav()),
            Theme.scroll(content));
    }


    /** The "What are you craving?" box that searches by dish. */
    private JComponent cravingCard()
    {
        Theme.RoundedPanel card = Theme.card(null);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JTextField dishField = Theme.styleField(new JTextField(22));
        JButton findButton = Theme.button("Find it", Theme.ButtonStyle.PRIMARY);
        JPanel row = Theme.clear(new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0)));
        row.add(Theme.text("What are you craving?", Font.BOLD, 17, Theme.MAROON));
        row.add(dishField);
        row.add(findButton);
        card.add(Theme.fullWidth(row));

        JLabel hint = Theme.text("Search by dish, like " + DISH_EXAMPLES + ".", Font.PLAIN, 13, Theme.GRAY);
        JPanel hintRow = Theme.clear(new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6)));
        hintRow.add(hint);
        card.add(Theme.fullWidth(hintRow));

        Runnable find = () -> {
            String dish = dishField.getText();
            if (dish.isBlank() || !moderator.validateInput(dish))
            {
                hint.setText("Type a dish with at least one letter, like tacos or pad thai.");
                hint.setForeground(Theme.ERROR);
                return;
            }
            openSearch(visibleRestaurants(), dish.trim());
        };
        findButton.addActionListener(e -> find.run());
        dishField.addActionListener(e -> find.run());
        return Theme.fullWidth(card);
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
     *            redraws the current screen after the user eats at or
     *            blacklists the restaurant
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
            titleRow.add(Theme.chip("Favorite", Theme.SUCCESS_TINT, Theme.SUCCESS));
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
        JButton blacklistButton = Theme.button("Blacklist", Theme.ButtonStyle.GHOST);
        blacklistButton.setToolTipText("Never show this restaurant again");
        blacklistButton.addActionListener(e -> handleBlacklist(restaurant, refresh));

        JPanel buttons = Theme.clear(new JPanel(new GridLayout(2, 1, 0, 6)));
        buttons.add(eatButton);
        buttons.add(blacklistButton);
        JPanel buttonHolder = Theme.clear(new JPanel(new BorderLayout()));
        buttonHolder.add(buttons, BorderLayout.NORTH);

        card.add(info, BorderLayout.CENTER);
        card.add(buttonHolder, BorderLayout.EAST);
        return Theme.fullWidth(card);
    }


    /**
     * "Eat here" saves the visit as pending. The post-visit survey page comes
     * up the next time the app opens, and finishing it clears the visit.
     */
    private void handleEatHere(Restaurant restaurant, Runnable refresh)
    {
        if (database == null || currentProfile.getEmail().isBlank())
        {
            flash("Visits can't be saved right now, so we can't ask about this one later.", true);
            refresh.run();
            return;
        }
        try
        {
            database.addPendingVisit(currentProfile.getEmail(), restaurant);
            flash("Enjoy " + restaurant.getName()
                + "! Next time you open Hungry Hokie, we'll ask how it was.");
        }
        catch (IllegalArgumentException | IllegalStateException e)
        {
            warn("Couldn't save the visit", e);
            flash("Couldn't save that visit. Please try again.", true);
        }
        refresh.run();
    }


    private void handleBlacklist(Restaurant restaurant, Runnable refresh)
    {
        currentProfile.addToBlacklist(restaurant);
        saveProfile();
        flash("Blacklisted " + restaurant.getName()
            + ". You can take it off your blacklist on My Profile.");
        refresh.run();
    }


    // ---------------------------------------------------------------- Post-visit survey

    /**
     * Shows the post-visit survey page if the user clicked "Eat here" in an
     * earlier session.
     *
     * @return true if the page is now showing
     */
    private boolean showPostVisitSurveys()
    {
        refreshPendingVisits();
        if (pendingVisits.isEmpty())
        {
            return false;
        }
        showPostVisitPage(new ArrayList<>(pendingVisits), 0, 0);
        return true;
    }


    /**
     * Shows one post-visit survey page, then the next, then home.
     */
    private void showPostVisitPage(ArrayList<Restaurant> visits, int index, int rated)
    {
        if (index >= visits.size())
        {
            if (rated > 0)
            {
                flash("Thanks! Your " + (rated == 1 ? "rating" : rated + " ratings")
                    + " updated your recommendations.");
            }
            displayHomeScreen();
            return;
        }
        Restaurant restaurant = visits.get(index);
        PostVisitPage page = new PostVisitPage(restaurant, index + 1, visits.size(), moderator,
            (finished, choice) -> {
                if (choice == PostVisitPage.Choice.SUBMITTED)
                {
                    applyRating(restaurant, finished.getStars(), finished.getReview(),
                        finished.wouldGoBack());
                }
                if (choice != PostVisitPage.Choice.LATER)
                {
                    clearPendingVisit(restaurant);
                }
                int nowRated = rated + (choice == PostVisitPage.Choice.SUBMITTED ? 1 : 0);
                showPostVisitPage(visits, index + 1, nowRated);
            });
        showCard(POST_VISIT_CARD, page);
    }


    private void clearPendingVisit(Restaurant restaurant)
    {
        if (database == null)
        {
            return;
        }
        try
        {
            database.removePendingVisit(currentProfile.getEmail(), restaurant);
        }
        catch (IllegalStateException e)
        {
            warn("Couldn't clear the visit", e);
        }
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
        openSearch(results, "");
    }


    /** The search screen's inputs, so a search can be re-run. */
    private static class SearchForm
    {
        private JTextField name;
        private JTextField dish;
        private JComboBox<String> cuisine;
        private JComboBox<String> price;
        private JTextField miles;
        private JLabel error;
        private JPanel results;
    }


    /**
     * Opens the search screen.
     *
     * @param dish
     *            a dish to search for right away, or "" for none
     */
    private void openSearch(ArrayList<Restaurant> results, String dish)
    {
        refreshPendingVisits();
        SearchForm form = new SearchForm();
        form.name = Theme.styleField(new JTextField(10));
        form.dish = Theme.styleField(new JTextField(dish, 10));
        form.cuisine = Theme.styleCombo(new JComboBox<>(cuisineChoices()));
        form.price = Theme.styleCombo(new JComboBox<>(PRICE_CHOICES));
        form.miles = Theme.styleField(new JTextField(5));
        form.error = Theme.text(" ", Font.BOLD, 13, Theme.ERROR);
        form.results = Theme.clear(new JPanel(new BorderLayout()));

        Theme.RoundedPanel filters = Theme.card(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(3, 6, 3, 6);
        String[] labels = {"Name contains", "Dish (like tacos)", "Cuisine", "Max price", "Max distance (miles)"};
        JComponent[] inputs = {form.name, form.dish, form.cuisine, form.price, form.miles};
        for (int i = 0; i < inputs.length; i++)
        {
            c.gridx = i;
            c.gridy = 0;
            filters.add(Theme.text(labels[i], Font.BOLD, 13, Theme.GRAY_DARK), c);
            c.gridy = 1;
            filters.add(inputs[i], c);
        }

        JButton searchButton = Theme.button("Search", Theme.ButtonStyle.PRIMARY);
        JButton clearButton = Theme.button("Clear", Theme.ButtonStyle.GHOST);
        JPanel actions = Theme.clear(new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0)));
        actions.add(searchButton);
        actions.add(clearButton);
        actions.add(Box.createHorizontalStrut(8));
        actions.add(form.error);
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = inputs.length;
        c.insets = new Insets(12, 0, 0, 0);
        filters.add(actions, c);

        Runnable showAll = () -> displaySearchResults(visibleRestaurants());
        searchNote = "";
        form.results.add(Theme.scroll(createResultsView(results, showAll)), BorderLayout.CENTER);

        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setBackground(Theme.BACKGROUND);
        body.setBorder(new EmptyBorder(18, 24, 0, 24));
        body.add(filters, BorderLayout.NORTH);
        body.add(form.results, BorderLayout.CENTER);

        Runnable search = () -> runSearch(form);
        searchButton.addActionListener(e -> search.run());
        form.name.addActionListener(e -> search.run());
        form.dish.addActionListener(e -> search.run());
        form.miles.addActionListener(e -> search.run());
        clearButton.addActionListener(e -> showAll.run());

        showScreen(
            RESULTS_CARD,
            Theme.headerBar("Search", "Filter by name, dish, cuisine, price, and distance",
                homeNav(), profileNav()),
            body);
        if (!dish.isBlank())
        {
            search.run();
        }
    }


    /**
     * Runs the search with whatever filters are filled in. Bad input shows a
     * red message instead of crashing.
     */
    private void runSearch(SearchForm form)
    {
        ArrayList<Restaurant> results;
        try
        {
            results = filterRestaurants(form);
        }
        catch (IllegalArgumentException e)
        {
            form.error.setText(e.getMessage());
            return;
        }
        form.error.setText(" ");
        refreshPendingVisits();

        form.results.removeAll();
        form.results.add(Theme.scroll(createResultsView(results, () -> runSearch(form))),
            BorderLayout.CENTER);
        form.results.revalidate();
        form.results.repaint();
    }


    /**
     * Applies each filter the user filled in, one Search at a time.
     *
     * @throws IllegalArgumentException
     *             with a message for the user if any filter is invalid
     */
    private ArrayList<Restaurant> filterRestaurants(SearchForm form)
    {
        searchNote = "";
        ArrayList<Restaurant> results = visibleRestaurants();
        String name = form.name.getText();
        if (!name.isBlank())
        {
            if (!moderator.validateInput(name))
            {
                throw new IllegalArgumentException(
                    "Restaurant names need at least one letter or number.");
            }
            results = new Search(results).searchByName(name);
        }
        String dish = form.dish.getText();
        if (!dish.isBlank())
        {
            if (!moderator.validateInput(dish))
            {
                throw new IllegalArgumentException("Dishes need at least one letter or number.");
            }
            ArrayList<String> cuisines = Search.cuisinesForDish(dish);
            results = new Search(results).searchByDish(dish);
            if (cuisines.isEmpty() && results.isEmpty())
            {
                throw new IllegalArgumentException(
                    "We don't know \"" + dish.trim() + "\" yet. Try " + DISH_EXAMPLES + ".");
            }
            if (!cuisines.isEmpty())
            {
                searchNote = "Places that usually serve " + dish.trim() + ": "
                    + String.join(", ", cuisines) + " restaurants";
            }
        }
        String cuisine = (String)form.cuisine.getSelectedItem();
        if (cuisine != null && !cuisine.equals(ANY_CUISINE))
        {
            results = new Search(results).searchByTag(new Tag(cuisine));
        }
        int maxPrice = form.price.getSelectedIndex();
        if (maxPrice > 0)
        {
            results = new Search(results).searchByPrice(maxPrice);
        }
        String milesText = form.miles.getText();
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
        if (searchNote != null && !searchNote.isBlank())
        {
            view.add(Theme.fullWidth(Theme.text(searchNote, Font.PLAIN, 13, Theme.GRAY)));
        }
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
     * Displays everything the app has saved about the user: photo, name,
     * age, flavor profile, favorite cuisines, favorite restaurants,
     * blacklisted restaurants, and their rating history.
     */
    public void displayProfile()
    {
        refreshPendingVisits();
        JPanel content = pageContent();
        addFlashBanner(content);
        content.add(Box.createVerticalStrut(16));
        content.add(profileTopCard());

        content.add(Theme.sectionHeader("About you"));
        content.add(aboutCard());

        content.add(Theme.sectionHeader("Flavor profile"));
        Theme.RoundedPanel flavors = Theme.card(new GridBagLayout());
        addFlavorRows(flavors, 0, currentProfile.getFlavorProfile(), false, null);
        content.add(Theme.fullWidth(flavors));

        content.add(Theme.sectionHeader("Favorite cuisines"));
        JPanel cuisineChips = Theme.clear(new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0)));
        ArrayList<String> cuisineNames = new ArrayList<>();
        for (Tag tag : currentProfile.getTasteProfile())
        {
            cuisineNames.add(tag.getName());
        }
        for (String cuisine : currentProfile.getFavoriteCuisines())
        {
            if (!cuisineNames.contains(cuisine))
            {
                cuisineNames.add(cuisine);
            }
        }
        if (cuisineNames.isEmpty())
        {
            content.add(messageCard("None yet. Pick one with Edit profile."));
        }
        else
        {
            for (String cuisine : cuisineNames)
            {
                cuisineChips.add(Theme.chip(cuisine, Theme.MAROON_TINT, Theme.MAROON));
            }
            content.add(cardAround(cuisineChips));
        }

        content.add(Theme.sectionHeader("Favorite restaurants"));
        ArrayList<Restaurant> favorites = currentProfile.getFavoriteRestaurants();
        if (favorites.isEmpty())
        {
            content.add(messageCard(
                "None yet. Check \"I'd go back here\" on the post-visit survey to add one."));
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

        content.add(Theme.sectionHeader("Blacklisted restaurants"));
        ArrayList<Restaurant> blacklisted = currentProfile.getBlacklist().getBlacklistedRestaurants();
        if (blacklisted.isEmpty())
        {
            content.add(messageCard(
                "Your blacklist is empty. Click \"Blacklist\" on a restaurant you never want to see."));
        }
        else
        {
            JPanel blacklist = listPanel();
            for (Restaurant restaurant : blacklisted)
            {
                JButton removeButton = Theme.button("Remove", Theme.ButtonStyle.SECONDARY);
                removeButton.addActionListener(e -> {
                    currentProfile.getBlacklist().removeRestaurant(restaurant);
                    saveProfile();
                    flash(restaurant.getName() + " is off your blacklist.");
                    displayProfile();
                });
                addListRow(blacklist, restaurant.getName(), resolve(restaurant).getAddress(), removeButton);
            }
            content.add(cardAround(blacklist));
        }

        if (!pendingVisits.isEmpty())
        {
            content.add(Theme.sectionHeader("Waiting for your rating"));
            JPanel pendingList = listPanel();
            for (Restaurant restaurant : pendingVisits)
            {
                addListRow(pendingList, restaurant.getName(),
                    "We'll ask how it was the next time you open the app", null);
            }
            content.add(cardAround(pendingList));
        }

        content.add(Theme.sectionHeader("What Hungry Hokie has learned from your ratings"));
        content.add(learnedCard());

        content.add(Theme.sectionHeader("Your ratings"));
        content.add(ratingsCard());

        showScreen(
            PROFILE_CARD,
            Theme.headerBar("My Profile", "Everything Hungry Hokie has saved about you",
                homeNav(), searchNav()),
            Theme.scroll(content));
    }


    /** Photo, name, and email, with buttons to change the photo or sign out. */
    private JComponent profileTopCard()
    {
        Theme.RoundedPanel card = Theme.card(new BorderLayout(22, 0));
        card.setBorder(new EmptyBorder(20, 22, 20, 22));
        byte[] photo = photoBytes();
        card.add(new Theme.Avatar(toImage(photo), currentProfile.getName(), 112), BorderLayout.WEST);

        JPanel middle = Theme.clear(new JPanel());
        middle.setLayout(new BoxLayout(middle, BoxLayout.Y_AXIS));
        middle.add(Theme.text(currentProfile.getName(), Font.BOLD, 26, Theme.BLACK));
        middle.add(Theme.text(currentProfile.getEmail(), Font.PLAIN, 15, Theme.GRAY));
        middle.add(Box.createVerticalStrut(12));
        JPanel photoButtons = Theme.clear(new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)));
        JButton uploadButton = Theme.button(photo == null ? "Upload photo" : "Change photo",
            Theme.ButtonStyle.SECONDARY);
        uploadButton.addActionListener(e -> choosePhoto());
        photoButtons.add(uploadButton);
        if (photo != null)
        {
            JButton removeButton = Theme.button("Remove photo", Theme.ButtonStyle.GHOST);
            removeButton.addActionListener(e -> removePhoto());
            photoButtons.add(Box.createHorizontalStrut(8));
            photoButtons.add(removeButton);
        }
        photoButtons.setAlignmentX(0);
        middle.add(photoButtons);
        JPanel middleHolder = Theme.clear(new JPanel(new GridBagLayout()));
        GridBagConstraints left = new GridBagConstraints();
        left.anchor = GridBagConstraints.WEST;
        left.weightx = 1;
        middleHolder.add(middle, left);
        card.add(middleHolder, BorderLayout.CENTER);

        JButton editButton = Theme.button("Edit profile", Theme.ButtonStyle.SECONDARY);
        editButton.addActionListener(e -> displaySurvey());
        JButton signOutButton = Theme.button("Sign out", Theme.ButtonStyle.GHOST);
        signOutButton.addActionListener(e -> signOut());
        JPanel accountButtons = Theme.clear(new JPanel(new GridLayout(2, 1, 0, 6)));
        accountButtons.add(editButton);
        accountButtons.add(signOutButton);
        JPanel accountHolder = Theme.clear(new JPanel(new GridBagLayout()));
        accountHolder.add(accountButtons);
        card.add(accountHolder, BorderLayout.EAST);
        return Theme.fullWidth(card);
    }


    private JComponent aboutCard()
    {
        Theme.RoundedPanel about = Theme.card(new GridBagLayout());
        int row = 0;
        addInfoRow(about, row++, "Name", Theme.text(currentProfile.getName(), Font.PLAIN, 15, Theme.BLACK));
        addInfoRow(about, row++, "Age", Theme.text(
            currentProfile.getAge() == 0 ? "Not set" : String.valueOf(currentProfile.getAge()),
            Font.PLAIN, 15, Theme.BLACK));
        addInfoRow(about, row++, "Email", Theme.text(currentProfile.getEmail(), Font.PLAIN, 15, Theme.BLACK));
        addInfoRow(about, row++, "Location", Theme.text(displayLocation(), Font.PLAIN, 15, Theme.BLACK));

        Boolean permission = locationPermission();
        String access = Boolean.TRUE.equals(permission) ? "Allowed"
            : Boolean.FALSE.equals(permission) ? "Not allowed" : "Not asked yet";
        JPanel accessRow = Theme.clear(new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)));
        accessRow.add(Theme.text(access, Font.PLAIN, 15, Theme.BLACK));
        accessRow.add(Box.createHorizontalStrut(14));
        if (Boolean.TRUE.equals(permission))
        {
            JButton offButton = Theme.button("Turn off", Theme.ButtonStyle.GHOST);
            offButton.addActionListener(e -> turnOffLocation());
            accessRow.add(offButton);
        }
        else
        {
            JButton onButton = Theme.button("Use my current location", Theme.ButtonStyle.SECONDARY);
            onButton.addActionListener(e -> turnOnLocation(onButton));
            accessRow.add(onButton);
        }
        addInfoRow(about, row++, "Location access", accessRow);

        String diets = currentProfile.getDietaryRestrictions().isEmpty()
            ? "None"
            : String.join(", ", currentProfile.getDietaryRestrictions());
        addInfoRow(about, row++, "Dietary restriction", Theme.text(diets, Font.PLAIN, 15, Theme.BLACK));
        String storage = database == null
            ? "Not saved (the database couldn't be opened)"
            : "Saved on this computer in " + DATABASE_FILE;
        addInfoRow(about, row++, "Stored", Theme.text(storage, Font.PLAIN, 13, Theme.GRAY));
        return Theme.fullWidth(about);
    }


    /**
     * Adds one row per flavor: its name, five dots, and a word for the level.
     *
     * @param pickers
     *            if not null, each editable picker is stored here by flavor
     * @return the next free row
     */
    private static int addFlavorRows(
        JPanel panel,
        int row,
        LinkedHashMap<String, Integer> levels,
        boolean editable,
        Map<String, Theme.LevelPicker> pickers)
    {
        for (String flavor : Profile.FLAVORS)
        {
            int level = levels.getOrDefault(flavor, Profile.DEFAULT_FLAVOR_LEVEL);
            Theme.LevelPicker picker = new Theme.LevelPicker(level, editable, 22);
            JLabel words = Theme.text(Theme.LevelPicker.WORDS[level], Font.PLAIN, 14, Theme.GRAY_DARK);
            picker.setOnChange(() -> words.setText(Theme.LevelPicker.WORDS[picker.getStars()]));
            if (pickers != null)
            {
                pickers.put(flavor, picker);
            }

            GridBagConstraints c = new GridBagConstraints();
            c.gridy = row++;
            c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(5, 0, 5, 18);
            c.gridx = 0;
            JLabel name = Theme.text(flavor, Font.BOLD, 14, Theme.BLACK);
            name.setPreferredSize(new Dimension(70, name.getPreferredSize().height));
            panel.add(name, c);
            c.gridx = 1;
            panel.add(picker, c);
            c.gridx = 2;
            c.weightx = 1;
            panel.add(words, c);
        }
        return row;
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
            return messageCard("Ratings aren't saved because the database couldn't be opened.");
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
        sessionPhoto = null;
        displaySurvey();
    }


    // ---------------------------------------------------------------- Profile photo

    private void choosePhoto()
    {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose a photo of yourself");
        chooser.addChoosableFileFilter(
            new FileNameExtensionFilter("Photos (.jpg, .jpeg, .png)", "jpg", "jpeg", "png"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
        {
            return;
        }
        String problem = savePhoto(chooser.getSelectedFile());
        if (problem == null)
        {
            flash("Profile photo updated.");
        }
        else
        {
            flash(problem, true);
        }
        displayProfile();
    }


    /**
     * Checks, crops, and saves a profile photo.
     *
     * @return null if it worked, or a message saying what's wrong
     */
    private String savePhoto(File file)
    {
        if (file == null || !moderator.moderateReviewPhoto(file.getName()))
        {
            return "That file isn't a photo. Pick a .jpg or .png image.";
        }
        if (file.length() > MAX_PHOTO_BYTES)
        {
            return "That photo is too big. Pick one under 15 MB.";
        }
        BufferedImage image;
        try
        {
            image = ImageIO.read(file);
        }
        catch (IOException e)
        {
            image = null;
        }
        if (image == null)
        {
            return "We couldn't open that photo. Try a different .jpg or .png.";
        }
        byte[] png = squarePng(image);
        if (png == null)
        {
            return "We couldn't process that photo. Try a different one.";
        }
        if (database == null)
        {
            sessionPhoto = png;
            return null;
        }
        try
        {
            database.savePhoto(currentProfile.getEmail(), png);
            return null;
        }
        catch (IllegalArgumentException | IllegalStateException e)
        {
            warn("Couldn't save the photo", e);
            return "Couldn't save your photo. Please try again.";
        }
    }


    /** Crops a photo to a centered square and shrinks it to PHOTO_SIZE. */
    private static byte[] squarePng(BufferedImage image)
    {
        int side = Math.min(image.getWidth(), image.getHeight());
        int x = (image.getWidth() - side) / 2;
        int y = (image.getHeight() - side) / 2;
        BufferedImage square = new BufferedImage(PHOTO_SIZE, PHOTO_SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = square.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, PHOTO_SIZE, PHOTO_SIZE);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(image, 0, 0, PHOTO_SIZE, PHOTO_SIZE, x, y, x + side, y + side, null);
        g.dispose();
        try
        {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(square, "png", out);
            return out.toByteArray();
        }
        catch (IOException e)
        {
            return null;
        }
    }


    private void removePhoto()
    {
        sessionPhoto = null;
        if (database != null)
        {
            try
            {
                database.removePhoto(currentProfile.getEmail());
            }
            catch (IllegalStateException e)
            {
                warn("Couldn't remove the photo", e);
            }
        }
        flash("Profile photo removed.");
        displayProfile();
    }


    private byte[] photoBytes()
    {
        if (database == null)
        {
            return sessionPhoto;
        }
        try
        {
            return database.loadPhoto(currentProfile.getEmail());
        }
        catch (IllegalStateException e)
        {
            warn("Couldn't load the photo", e);
            return null;
        }
    }


    private static BufferedImage toImage(byte[] bytes)
    {
        if (bytes == null)
        {
            return null;
        }
        try
        {
            return ImageIO.read(new ByteArrayInputStream(bytes));
        }
        catch (IOException e)
        {
            return null;
        }
    }


    // ---------------------------------------------------------------- Live location

    /**
     * Asks the user before the app ever reads their location. Once they
     * allow it, the answer is remembered; they can turn it off on My Profile.
     *
     * @return true if location access is allowed
     */
    private boolean askLocationPermission()
    {
        if (Boolean.TRUE.equals(locationPermission()))
        {
            return true;
        }
        boolean allowed = Theme.confirm(this, "Allow location access?",
            "Hungry Hokie would like to use your current location to show how far away each "
                + "restaurant is and recommend places near you.\n"
                + "Your location stays on this computer. You can turn this off anytime on My Profile.",
            "Allow", "Don't allow");
        storeLocationPermission(allowed);
        return allowed;
    }


    /**
     * Asks permission if needed, then finds the user's live location in the
     * background so the window doesn't freeze.
     *
     * @param busyButton
     *            shows "Finding your location..." while it works
     * @param onFound
     *            gets {latitude, longitude}
     * @param onFailed
     *            gets a message for the user
     */
    private void requestLiveLocation(
        JButton busyButton,
        Consumer<double[]> onFound,
        Consumer<String> onFailed)
    {
        if (!LocationFinder.isSupported())
        {
            onFailed.accept("Live location only works on Windows. Pick your town instead.");
            return;
        }
        if (!askLocationPermission())
        {
            onFailed.accept("Location access is off, so pick your town instead.");
            return;
        }
        String label = busyButton.getText();
        busyButton.setEnabled(false);
        busyButton.setText("Finding your location...");
        new SwingWorker<double[], Void>()
        {
            @Override
            protected double[] doInBackground()
            {
                return LocationFinder.findCurrentLocation();
            }


            @Override
            protected void done()
            {
                busyButton.setEnabled(true);
                busyButton.setText(label);
                double[] found = foundLocation(this);
                if (found == null)
                {
                    onFailed.accept("We couldn't find your location. Make sure location is on in "
                        + "Windows settings, or pick your town.");
                }
                else
                {
                    onFound.accept(found);
                }
            }
        }.execute();
    }


    /**
     * Updates a returning user's live location in the background when the
     * app opens, if they've allowed it.
     */
    private void refreshLiveLocation()
    {
        if (!CURRENT_LOCATION.equals(currentProfile.getLocation())
            || !Boolean.TRUE.equals(locationPermission())
            || !LocationFinder.isSupported())
        {
            return;
        }
        Profile user = currentProfile;
        new SwingWorker<double[], Void>()
        {
            @Override
            protected double[] doInBackground()
            {
                return LocationFinder.findCurrentLocation();
            }


            @Override
            protected void done()
            {
                double[] found = foundLocation(this);
                if (found == null || user != currentProfile)
                {
                    return;
                }
                currentProfile.setCoordinates(found[0], found[1]);
                saveProfile();
                updateDistances();
                if (HOME_CARD.equals(currentCard))
                {
                    displayHomeScreen();
                }
            }
        }.execute();
    }


    private static double[] foundLocation(SwingWorker<double[], Void> worker)
    {
        try
        {
            return worker.get();
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            return null;
        }
        catch (ExecutionException e)
        {
            return null;
        }
    }


    private void turnOnLocation(JButton button)
    {
        requestLiveLocation(button, found -> {
            currentProfile.setLocation(CURRENT_LOCATION);
            currentProfile.setCoordinates(found[0], found[1]);
            saveProfile();
            updateDistances();
            flash("Using your current location.");
            displayProfile();
        }, problem -> {
            flash(problem, true);
            displayProfile();
        });
    }


    private void turnOffLocation()
    {
        storeLocationPermission(false);
        if (CURRENT_LOCATION.equals(currentProfile.getLocation()))
        {
            String town = currentProfile.hasCoordinates()
                ? nearestTown(currentProfile.getLatitude(), currentProfile.getLongitude())
                : null;
            currentProfile.setLocation(town == null ? TOWN_NAMES[0] : town);
            currentProfile.clearCoordinates();
            saveProfile();
            updateDistances();
            flash("Location access is off. Distances are now measured from "
                + currentProfile.getLocation() + ".");
        }
        else
        {
            flash("Location access is off.");
        }
        displayProfile();
    }


    private Boolean locationPermission()
    {
        if (database == null)
        {
            return sessionLocationPermission;
        }
        try
        {
            return database.getLocationPermission();
        }
        catch (IllegalStateException e)
        {
            warn("Couldn't load location permission", e);
            return sessionLocationPermission;
        }
    }


    private void storeLocationPermission(boolean allowed)
    {
        sessionLocationPermission = allowed;
        if (database != null)
        {
            try
            {
                database.setLocationPermission(allowed);
            }
            catch (IllegalStateException e)
            {
                warn("Couldn't save location permission", e);
            }
        }
    }


    // ---------------------------------------------------------------- Survey

    /** Displays the survey. */
    public void displaySurvey()
    {
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
        GridBagConstraints wide = new GridBagConstraints();
        wide.gridx = 0;
        wide.gridwidth = 3;
        wide.fill = GridBagConstraints.HORIZONTAL;
        wide.weightx = 1;
        wide.gridy = 0;
        wide.insets = new Insets(0, 0, 4, 0);
        form.add(Theme.text(editing ? "Update your profile" : "Tell us about you",
            Font.BOLD, 22, Theme.MAROON), wide);
        wide.gridy = 1;
        wide.insets = new Insets(0, 0, 18, 0);
        form.add(Theme.text("We use this to pick restaurants you'll like.",
            Font.PLAIN, 14, Theme.GRAY), wide);

        int row = addFormField(form, 2, 0, 2, "Name", nameField);
        row = addFormField(form, row, 0, 2, "Email (so we remember your ratings next time)", emailField);
        addFormField(form, row, 0, 1, "Age", ageCombo);
        row = addFormField(form, row, 1, 1, "Location", locationCombo);
        addFormField(form, row, 0, 1, "Dietary restriction", dietaryCombo);
        row = addFormField(form, row, 1, 1, "Favorite cuisine", cuisineCombo);

        wide.gridy = row++;
        wide.insets = new Insets(6, 0, 8, 0);
        form.add(Theme.text("How much do you like each flavor?", Font.BOLD, 13, Theme.GRAY_DARK), wide);
        JPanel flavorPanel = Theme.clear(new JPanel(new GridBagLayout()));
        LinkedHashMap<String, Theme.LevelPicker> flavorPickers = new LinkedHashMap<>();
        addFlavorRows(flavorPanel, 0, currentProfile.getFlavorProfile(), true, flavorPickers);
        wide.gridy = row++;
        wide.insets = new Insets(0, 0, 8, 0);
        form.add(flavorPanel, wide);

        wide.gridy = row++;
        wide.insets = new Insets(0, 0, 0, 0);
        JLabel errorLabel = Theme.text(" ", Font.BOLD, 13, Theme.ERROR);
        form.add(errorLabel, wide);

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
        wide.gridy = row++;
        wide.insets = new Insets(10, 0, 0, 0);
        form.add(buttons, wide);
        wide.gridy = row++;
        form.add(Box.createHorizontalStrut(500), wide);

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
            errorLabel.setText(" ");

            Survey survey = new Survey();
            survey.setName(name.trim());
            survey.setEmail(email.trim());
            survey.setAge((Integer)ageValue);
            survey.setLocation(location.trim());
            String dietaryChoice = (String)dietaryCombo.getSelectedItem();
            if (dietaryChoice != null && !dietaryChoice.equals(NO_DIET))
            {
                survey.addDietaryRestriction(dietaryChoice);
            }
            String cuisineChoice = (String)cuisineCombo.getSelectedItem();
            if (cuisineChoice != null && !cuisineChoice.trim().isEmpty())
            {
                survey.addTag(cuisineChoice.trim());
            }
            flavorPickers.forEach((flavor, picker) -> survey.setFlavorLevel(flavor, picker.getStars()));

            if (CURRENT_LOCATION.equals(location))
            {
                requestLiveLocation(submitButton,
                    found -> finishSurvey(survey, editing, found),
                    errorLabel::setText);
                return;
            }
            finishSurvey(survey, editing, null);
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
     * Turns the survey into the user's profile, keeping what the app already
     * knew about their email, then opens their home page.
     *
     * @param liveCoordinates
     *            {latitude, longitude} if they chose their current location
     */
    private void finishSurvey(Survey survey, boolean editing, double[] liveCoordinates)
    {
        currentSurvey = survey;
        Profile newProfile = survey.toProfile();
        if (liveCoordinates != null)
        {
            newProfile.setCoordinates(liveCoordinates[0], liveCoordinates[1]);
        }
        mergeSavedData(newProfile);
        currentProfile = newProfile;
        saveProfile();
        rememberSignedIn();
        if (editing)
        {
            updateDistances();
            flash("Profile saved.");
            displayProfile();
            return;
        }
        openFor(newProfile, false);
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
     * preferences, favorites, and blacklisted restaurants.
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
     * Measures every restaurant's distance from the user, or marks it
     * unknown if we don't know where they are.
     */
    private void updateDistances()
    {
        double[] here = userCoordinates();
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
     * @return {latitude, longitude} of the user's live location or chosen
     *         town, or null if unknown
     */
    private double[] userCoordinates()
    {
        String location = currentProfile.getLocation();
        if (CURRENT_LOCATION.equals(location))
        {
            return currentProfile.hasCoordinates()
                ? new double[] {currentProfile.getLatitude(), currentProfile.getLongitude()}
                : null;
        }
        for (int i = 0; i < TOWN_NAMES.length; i++)
        {
            if (TOWN_NAMES[i].equalsIgnoreCase(location))
            {
                return TOWN_COORDINATES[i];
            }
        }
        return null;
    }


    /**
     * @return the user's location for display, like "Blacksburg" or "Your
     *         location, near Blacksburg"
     */
    private String displayLocation()
    {
        String location = currentProfile.getLocation();
        if (!CURRENT_LOCATION.equals(location))
        {
            return location;
        }
        if (!currentProfile.hasCoordinates())
        {
            return "Your location";
        }
        String town = nearestTown(currentProfile.getLatitude(), currentProfile.getLongitude());
        return town == null ? "Your location" : "Your location, near " + town;
    }


    /**
     * @return the closest town within 15 miles, or null if none are
     */
    private static String nearestTown(double latitude, double longitude)
    {
        String closest = null;
        double closestMiles = 15;
        for (int i = 0; i < TOWN_NAMES.length; i++)
        {
            double miles = DistanceCalculator.straightLineDistance(
                latitude, longitude, TOWN_COORDINATES[i][0], TOWN_COORDINATES[i][1]);
            if (miles < closestMiles)
            {
                closest = TOWN_NAMES[i];
                closestMiles = miles;
            }
        }
        return closest;
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


    private void flash(String message)
    {
        flash(message, false);
    }


    /**
     * Shows a message at the top of the next screen that's drawn.
     *
     * @param error
     *            true for a red problem message
     */
    private void flash(String message, boolean error)
    {
        flashMessage = message;
        flashIsError = error;
    }


    private void addFlashBanner(JPanel content)
    {
        if (flashMessage != null)
        {
            content.add(Box.createVerticalStrut(12));
            content.add(Theme.banner(flashMessage, flashIsError));
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


    private static JComponent textRow(String text, int style, Color color)
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


    private static JComponent cardAround(JComponent inside)
    {
        Theme.RoundedPanel card = Theme.card(new BorderLayout());
        card.add(inside, BorderLayout.CENTER);
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
        currentCard = name;
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
