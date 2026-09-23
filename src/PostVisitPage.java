import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * The post-visit survey page. When a user clicks "Eat here", that visit is
 * saved as pending. The next time they open the app, this page comes up
 * before the home page and asks how the meal was. Finishing it clears the
 * pending visit, and the star rating tunes their recommendations.
 */
public class PostVisitPage extends JPanel {
    /** What the user chose. */
    public enum Choice {
        /** They rated it. */
        SUBMITTED,
        /** They never went, so forget the visit. */
        DIDNT_GO,
        /** Ask again next time the app opens. */
        LATER
    }

    /** Called when the user finishes the page. */
    public interface Listener {
        /**
         * @param page   the page, for reading the answers
         * @param choice what the user picked
         */
        void finished(PostVisitPage page, Choice choice);
    }

    private static final String[] STAR_WORDS =
        {"Tap a star to rate", "Didn't like it", "It was okay", "It was good", "Really good", "Loved it!"};

    private final Moderator moderator;
    private final Listener listener;
    private final Theme.StarRating starRating;
    private final JTextArea reviewArea;
    private final JCheckBox goBackBox;
    private final JLabel errorLabel;

    /**
     * @param restaurant where they said they'd eat
     * @param number     which pending visit this is, starting at 1
     * @param total      how many pending visits there are
     * @param moderator  checks the review text
     * @param listener   told when the user submits, skips, or says they didn't go
     */
    public PostVisitPage(Restaurant restaurant, int number, int total, Moderator moderator, Listener listener) {
        super(new BorderLayout());
        this.moderator = moderator;
        this.listener = listener;
        this.errorLabel = Theme.text(" ", Font.BOLD, 13, Theme.ERROR);
        setBackground(Theme.BACKGROUND);

        String subtitle = total > 1
            ? "Visit " + number + " of " + total + " from last time"
            : "You picked this place the last time you used Hungry Hokie";
        add(Theme.headerBar("Post-visit survey", subtitle), BorderLayout.NORTH);

        Theme.RoundedPanel card = Theme.card(new GridBagLayout());
        card.setBorder(new EmptyBorder(24, 28, 24, 28));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;

        c.insets = new Insets(0, 0, 6, 0);
        card.add(Theme.text("How was " + restaurant.getName() + "?", Font.BOLD, 24, Theme.MAROON), c);
        JPanel details = Theme.clear(new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0)));
        for (Tag tag : restaurant.getTags()) {
            details.add(Theme.chip(tag.getName(), Theme.MAROON_TINT, Theme.MAROON));
        }
        if (!restaurant.getAddress().isBlank()) {
            details.add(Theme.text(restaurant.getAddress(), Font.PLAIN, 13, Theme.GRAY));
        }
        c.insets = new Insets(0, -6, 20, 0);
        card.add(details, c);

        c.insets = new Insets(0, 0, 8, 0);
        card.add(Theme.text("Your rating", Font.BOLD, 14, Theme.BLACK), c);
        starRating = new Theme.StarRating(0, true, 40);
        JLabel starWords = Theme.text(STAR_WORDS[0], Font.PLAIN, 15, Theme.GRAY);
        starRating.setOnChange(() -> {
            starWords.setText(STAR_WORDS[starRating.getStars()]);
            starWords.setForeground(Theme.ORANGE_DARK);
            errorLabel.setText(" ");
        });
        JPanel starRow = Theme.clear(new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)));
        starRow.add(starRating);
        starRow.add(Box.createHorizontalStrut(16));
        starRow.add(starWords);
        c.insets = new Insets(0, 0, 20, 0);
        card.add(starRow, c);

        c.insets = new Insets(0, 0, 8, 0);
        card.add(Theme.text("Review (optional)", Font.BOLD, 14, Theme.BLACK), c);
        reviewArea = new JTextArea(4, 40);
        reviewArea.setFont(Theme.font(Font.PLAIN, 14));
        reviewArea.setLineWrap(true);
        reviewArea.setWrapStyleWord(true);
        reviewArea.setBorder(new EmptyBorder(8, 10, 8, 10));
        reviewArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                errorLabel.setText(" ");
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                errorLabel.setText(" ");
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                errorLabel.setText(" ");
            }
        });
        JScrollPane reviewScroll = new JScrollPane(reviewArea);
        reviewScroll.setBorder(BorderFactory.createLineBorder(Theme.GRAY_LIGHT, 1, true));
        c.insets = new Insets(0, 0, 14, 0);
        card.add(reviewScroll, c);

        goBackBox = new JCheckBox("I'd go back here");
        goBackBox.setFont(Theme.font(Font.PLAIN, 15));
        goBackBox.setOpaque(false);
        c.insets = new Insets(0, 0, 10, 0);
        card.add(goBackBox, c);
        card.add(errorLabel, c);

        JButton submit = Theme.button("Submit rating", Theme.ButtonStyle.PRIMARY);
        JButton later = Theme.button("Ask me later", Theme.ButtonStyle.SECONDARY);
        JButton didntGo = Theme.button("I didn't go", Theme.ButtonStyle.GHOST);
        submit.addActionListener(e -> submit());
        later.addActionListener(e -> listener.finished(this, Choice.LATER));
        didntGo.addActionListener(e -> listener.finished(this, Choice.DIDNT_GO));
        JPanel buttons = Theme.clear(new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)));
        buttons.add(submit);
        buttons.add(Box.createHorizontalStrut(10));
        buttons.add(later);
        buttons.add(Box.createHorizontalStrut(10));
        buttons.add(didntGo);
        c.insets = new Insets(8, 0, 0, 0);
        card.add(buttons, c);
        card.add(Box.createHorizontalStrut(520), c);

        JPanel centered = new JPanel(new GridBagLayout());
        centered.setBackground(Theme.BACKGROUND);
        centered.setBorder(new EmptyBorder(28, 24, 28, 24));
        GridBagConstraints middle = new GridBagConstraints();
        middle.anchor = GridBagConstraints.NORTH;
        middle.weighty = 1;
        centered.add(card, middle);
        add(Theme.scroll(centered), BorderLayout.CENTER);
    }

    /**
     * @return 1 to 5 once the user has picked a rating, or 0
     */
    public int getStars() {
        return starRating.getStars();
    }

    /**
     * @return their review, or "" for none
     */
    public String getReview() {
        return reviewArea.getText().trim();
    }

    /**
     * @return true if they said they'd go back
     */
    public boolean wouldGoBack() {
        return goBackBox.isSelected();
    }

    /** Checks the answers; bad input shows a red message and stays on the page. */
    private void submit() {
        if (starRating.getStars() == 0) {
            errorLabel.setText("Pick 1 to 5 stars first.");
            return;
        }
        String review = getReview();
        if (!review.isEmpty() && !moderator.moderateReviewText(review)) {
            errorLabel.setText("Please keep your review clean. It can't include inappropriate words.");
            return;
        }
        listener.finished(this, Choice.SUBMITTED);
    }
}
