import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

/**
 * Handles all UI
 */
public class Window
    extends JFrame
{

    private static String HOME_CARD = "HOME";
    private static String RESULTS_CARD = "RESULTS";
    private static String SURVEY_CARD = "SURVEY";

    private CardLayout cardLayout;
    private JPanel mainPanel;
    private Moderator moderator;
    private Profile currentProfile;
    private Survey currentSurvey;
    private Data yelpData;
    private ArrayList<Restaurant> localRestaurants;
    private Recommendations recommendationEngine;

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

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        add(mainPanel);
    }


    /** Displays the home page */
    public void displayHomeScreen()
    {
        if (currentProfile == null || currentProfile.getName().isBlank())
        {
            displaySurvey();
            return;
        }

        JPanel homePanel = new JPanel(new BorderLayout(10, 10));
        homePanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Welcome to Hungry Hokie", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        topPanel.add(title);

        JPanel summaryPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel profileLabel = new JLabel("Profile: " + currentProfile.getName()
            + " | " + currentProfile.getLocation() + " | "
            + currentProfile.getDietaryRestrictions());
        summaryPanel.add(profileLabel);
        topPanel.add(summaryPanel);
        homePanel.add(topPanel, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        JPanel sectionPanel = new JPanel();
        sectionPanel.setLayout(new BoxLayout(sectionPanel, BoxLayout.Y_AXIS));

        ArrayList<Restaurant> recommended = recommendationEngine.rankRestaurants(
            currentProfile, localRestaurants);

        sectionPanel.add(createSectionHeader("Recommendations"));
        sectionPanel.add(createRestaurantCardList(recommended));
        sectionPanel.add(Box.createVerticalStrut(20));
        sectionPanel.add(createSectionHeader("Restaurants in your Area"));
        sectionPanel.add(createRestaurantCardList(localRestaurants));

        JScrollPane scrollPane = new JScrollPane(sectionPanel);
        contentPanel.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 20));
        JButton searchButton = new JButton("Search Restaurants");
        JButton surveyButton = new JButton("Take Survey");
        buttonPanel.add(searchButton);
        buttonPanel.add(surveyButton);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);

        homePanel.add(contentPanel, BorderLayout.CENTER);

        surveyButton.addActionListener(e -> displaySurvey());
        searchButton.addActionListener(e -> displaySearchResults(localRestaurants));

        mainPanel.add(homePanel, HOME_CARD);
        cardLayout.show(mainPanel, HOME_CARD);
        setVisible(true);
    }

    private JPanel createSectionHeader(String title)
    {
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel header = new JLabel(title);
        header.setFont(new Font("SansSerif", Font.BOLD, 18));
        headerPanel.add(header);
        return headerPanel;
    }

    private JPanel createRestaurantCardList(ArrayList<Restaurant> restaurants)
    {
        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));

        if (restaurants == null || restaurants.isEmpty())
        {
            JLabel none = new JLabel("No restaurants found.");
            listPanel.add(none);
            return listPanel;
        }

        for (Restaurant restaurant : restaurants)
        {
            listPanel.add(createRestaurantCard(restaurant));
            listPanel.add(Box.createVerticalStrut(10));
        }
        return listPanel;
    }

    private JPanel createRestaurantCard(Restaurant restaurant)
    {
        JPanel card = new JPanel(new BorderLayout(8, 8));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)));

        JLabel nameLabel = new JLabel(restaurant.getName());
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 16));

        JLabel tagLabel = new JLabel("Tags: " + restaurant.getTags());
        JLabel detailLabel = new JLabel("Price: " + restaurant.getPriceSymbol() + "   Hours: "
            + getRestaurantHours(restaurant));
        JLabel bioLabel = new JLabel(getRestaurantBio(restaurant));

        JButton eatButton = new JButton("Eat here");
        eatButton.addActionListener(e -> handleEatHere(restaurant));

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.add(nameLabel);
        textPanel.add(tagLabel);
        textPanel.add(detailLabel);
        textPanel.add(bioLabel);

        card.add(textPanel, BorderLayout.CENTER);
        card.add(eatButton, BorderLayout.EAST);
        return card;
    }

    private String getRestaurantHours(Restaurant restaurant)
    {
        return yelpData.getHoursOpen(restaurant);
    }

    private String getRestaurantBio(Restaurant restaurant)
    {
        return yelpData.getBio(restaurant);
    }

    private void handleEatHere(Restaurant restaurant)
    {
        Object[] ratings = {"1", "2", "3", "4", "5"};
        String rating = (String) JOptionPane.showInputDialog(
            this,
            "How would you rate this restaurant?",
            "Post-visit survey",
            JOptionPane.PLAIN_MESSAGE,
            null,
            ratings,
            ratings[2]);

        if (rating == null) {
            return;
        }

        String comments = JOptionPane.showInputDialog(
            this,
            "What did you like most? (e.g. service, ambiance, flavor)",
            "Post-visit survey",
            JOptionPane.PLAIN_MESSAGE);

        int goBack = JOptionPane.showConfirmDialog(
            this,
            "Would you go back?",
            "Post-visit survey",
            JOptionPane.YES_NO_OPTION);

        if (comments != null && !comments.trim().isEmpty())
        {
            for (String part : comments.split(","))
            {
                String trimmed = part.trim();
                if (!trimmed.isEmpty())
                {
                    currentProfile.addTasteTag(new Tag(trimmed));
                    currentProfile.addTagWeight(new Tag(trimmed), Double.parseDouble(rating));
                }
            }
        }

        if (goBack == JOptionPane.YES_OPTION)
        {
            currentProfile.addFavoriteRestaurant(restaurant);
        }

        JOptionPane.showMessageDialog(this, "Thanks for visiting " + restaurant.getName() + "!");
    }


    /**
     * Displays a list of restaurant results
     * 
     * @param results
     *            an array list of restaurants
     */
    public void displaySearchResults(ArrayList<Restaurant> results)
    {
        JPanel resultsPanel = new JPanel(new BorderLayout(10, 10));
        resultsPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel header = new JLabel("Search Results", SwingConstants.CENTER);
        header.setFont(new Font("SansSerif", Font.BOLD, 20));
        resultsPanel.add(header, BorderLayout.NORTH);

        DefaultListModel<String> listModel = new DefaultListModel<>();
        if (results == null || results.isEmpty())
        {
            listModel.addElement("No restaurants found.");
        }
        else
        {
            for (Restaurant r : results)
            {
                listModel.addElement(r.getName() + "  —  " + r.getTags());
            }
        }
        JList<String> resultsList = new JList<>(listModel);
        resultsPanel.add(new JScrollPane(resultsList), BorderLayout.CENTER);

        JButton backButton = new JButton("Back to Home");
        backButton
            .addActionListener(e -> cardLayout.show(mainPanel, HOME_CARD));
        resultsPanel.add(backButton, BorderLayout.SOUTH);

        mainPanel.add(resultsPanel, RESULTS_CARD);
        cardLayout.show(mainPanel, RESULTS_CARD);
        setVisible(true);
    }


    /** Displays the survey. */
    public void displaySurvey()
    {
        currentSurvey = new Survey();

        JPanel surveyPanel = new JPanel();
        surveyPanel.setLayout(new BoxLayout(surveyPanel, BoxLayout.Y_AXIS));
        surveyPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel header = new JLabel("Preference Survey");
        header.setFont(new Font("SansSerif", Font.BOLD, 20));
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        surveyPanel.add(header);
        surveyPanel.add(Box.createVerticalStrut(15));

        JLabel nameLabel = new JLabel("Name:");
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JTextField nameField = new JTextField();
        nameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        surveyPanel.add(nameLabel);
        surveyPanel.add(nameField);
        surveyPanel.add(Box.createVerticalStrut(10));

        JLabel ageLabel = new JLabel("Age:");
        ageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JComboBox<Integer> ageCombo = new JComboBox<>();
        for (int i = 18; i <= 80; i++)
        {
            ageCombo.addItem(i);
        }
        ageCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        surveyPanel.add(ageLabel);
        surveyPanel.add(ageCombo);
        surveyPanel.add(Box.createVerticalStrut(10));

        JLabel locationLabel = new JLabel("Location:");
        locationLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JComboBox<String> locationCombo = new JComboBox<>(new String[]
        {
            "Blacksburg", "Christiansburg", "Roanoke", "Other"
        });
        locationCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        surveyPanel.add(locationLabel);
        surveyPanel.add(locationCombo);
        surveyPanel.add(Box.createVerticalStrut(10));

        JLabel dietaryLabel = new JLabel("Dietary restriction:");
        dietaryLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JComboBox<String> dietaryCombo = new JComboBox<>(new String[]
        {
            "None", "Vegetarian", "Vegan", "Gluten-Free", "Dairy-Free"
        });
        dietaryCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        surveyPanel.add(dietaryLabel);
        surveyPanel.add(dietaryCombo);
        surveyPanel.add(Box.createVerticalStrut(10));

        JLabel likesLabel = new JLabel("Preferred cuisine:");
        likesLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JComboBox<String> cuisineCombo = new JComboBox<>(new String[]
        {
            "Italian", "American", "Mexican", "Japanese", "Mediterranean",
            "Chinese", "Indian", "Vegan", "Thai", "Pizza", "BBQ"
        });
        cuisineCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        surveyPanel.add(likesLabel);
        surveyPanel.add(cuisineCombo);
        surveyPanel.add(Box.createVerticalStrut(15));

        JLabel statusLabel = new JLabel(" ");
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusLabel.setForeground(Color.RED);

        JButton submitButton = new JButton("Submit");
        submitButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        submitButton.addActionListener(e -> {
            String name = nameField.getText();
            String location = (String) locationCombo.getSelectedItem();
            Object ageValue = ageCombo.getSelectedItem();
            statusLabel.setForeground(Color.RED);

            if (!moderator.validateInput(name))
            {
                statusLabel.setText("Please enter a valid name.");
                return;
            }
            if (location == null || location.trim().isEmpty())
            {
                statusLabel.setText("Please select a location.");
                return;
            }
            if (ageValue == null)
            {
                statusLabel.setText("Please select an age.");
                return;
            }

            currentSurvey = new Survey();
            currentSurvey.setName(name.trim());
            currentSurvey.setAge((Integer) ageValue);
            currentSurvey.setLocation(location.trim());

            String dietaryChoice = (String) dietaryCombo.getSelectedItem();
            if (dietaryChoice != null && !dietaryChoice.equals("None"))
            {
                currentSurvey.addDietaryRestriction(dietaryChoice);
            }

            String cuisineChoice = (String) cuisineCombo.getSelectedItem();
            if (cuisineChoice != null && !cuisineChoice.trim().isEmpty())
            {
                currentSurvey.addTag(cuisineChoice.trim());
            }

            currentProfile = currentSurvey.toProfile();
            statusLabel.setForeground(new Color(0, 128, 0));
            statusLabel.setText("Preferences saved!");
            cardLayout.show(mainPanel, HOME_CARD);
            displayHomeScreen();
        });
        surveyPanel.add(submitButton);
        surveyPanel.add(Box.createVerticalStrut(10));
        surveyPanel.add(statusLabel);

        mainPanel.add(surveyPanel, SURVEY_CARD);
        cardLayout.show(mainPanel, SURVEY_CARD);
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
        Window window = new Window();
        window.displayHomeScreen();
    }
}
