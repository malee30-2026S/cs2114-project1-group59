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

    // ----------------------------------------------------------
    /**
     * Create a new Window object.
     */
    public Window()
    {
        super("Hungry Hokie");
        this.moderator = new Moderator();

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
        JPanel homePanel = new JPanel(new BorderLayout(10, 10));
        homePanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title =
            new JLabel("Welcome to Hungry Hokie", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        homePanel.add(title, BorderLayout.NORTH);

        JPanel buttonPanel =
            new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 20));
        JButton searchButton = new JButton("Search Restaurants");
        JButton surveyButton = new JButton("Take Survey");
        buttonPanel.add(searchButton);
        buttonPanel.add(surveyButton);
        homePanel.add(buttonPanel, BorderLayout.CENTER);

        surveyButton.addActionListener(e -> displaySurvey());
        searchButton
            .addActionListener(e -> displaySearchResults(new ArrayList<>()));

        mainPanel.add(homePanel, HOME_CARD);
        cardLayout.show(mainPanel, HOME_CARD);
        setVisible(true);
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
        JPanel surveyPanel = new JPanel();
        surveyPanel.setLayout(new BoxLayout(surveyPanel, BoxLayout.Y_AXIS));
        surveyPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel header = new JLabel("Preference Survey");
        header.setFont(new Font("SansSerif", Font.BOLD, 20));
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        surveyPanel.add(header);
        surveyPanel.add(Box.createVerticalStrut(15));

        JLabel cuisineLabel = new JLabel("Preferred cuisine:");
        cuisineLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JTextField cuisineField = new JTextField();
        cuisineField.setAlignmentX(Component.LEFT_ALIGNMENT);
        cuisineField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        surveyPanel.add(cuisineLabel);
        surveyPanel.add(cuisineField);
        surveyPanel.add(Box.createVerticalStrut(10));

        JLabel priceLabel = new JLabel("Preferred maximum price:");
        priceLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JTextField priceField = new JTextField();
        priceField.setAlignmentX(Component.LEFT_ALIGNMENT);
        priceField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        surveyPanel.add(priceLabel);
        surveyPanel.add(priceField);
        surveyPanel.add(Box.createVerticalStrut(15));

        JLabel statusLabel = new JLabel(" ");
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusLabel.setForeground(Color.RED);

        JButton submitButton = new JButton("Submit");
        submitButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        submitButton.addActionListener(e -> {
            String cuisine = cuisineField.getText();
            String price = priceField.getText();
            statusLabel.setForeground(Color.RED);

            if (!moderator.validateInput(cuisine))
            {
                statusLabel.setText("Please enter a valid cuisine.");
                return;
            }
            if (!moderator.validateInput(price))
            {
                statusLabel.setText(
                    "Please enter a valid maximum price (no special characters).");
                return;
            }

            int priceValue;
            try
            {
                priceValue = Integer.parseInt(price.trim());
            }
            catch (NumberFormatException ex)
            {
                statusLabel
                    .setText("Maximum price must be a non-negative whole number");
                return;
            }
            if (priceValue < 0)
            {
                statusLabel
                    .setText("Maximum price must be greater or equal to 0");
                return;
            }

            statusLabel.setForeground(new Color(0, 128, 0));
            statusLabel.setText("Preferences saved!");

            // TODO: make this do something

            cardLayout.show(mainPanel, HOME_CARD);
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
