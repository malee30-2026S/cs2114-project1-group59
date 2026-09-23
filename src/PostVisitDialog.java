import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * The post-visit survey. After a user picks "Eat here", this pops up the
 * next time they open the app and asks how the meal was. The star rating
 * then raises or lowers that restaurant's tags in their recommendations.
 */
public class PostVisitDialog extends JDialog {
    /** What the user chose. */
    public enum Choice {
        /** They rated it. */
        SUBMITTED,
        /** They never went, so forget the visit. */
        DIDNT_GO,
        /** Ask again next time the app opens. */
        LATER
    }

    private static final String[] STAR_WORDS =
        {"Tap a star to rate", "Didn't like it", "It was okay", "It was good", "Really good", "Loved it!"};

    private final Moderator moderator;
    private final Theme.StarRating starRating;
    private final JTextArea reviewArea;
    private final JCheckBox goBackBox;
    private final JLabel errorLabel;
    private Choice choice;

    /**
     * Builds the dialog. Call setVisible(true) to show it; it blocks until
     * the user answers.
     *
     * @param owner      the main window
     * @param restaurant where they ate
     * @param moderator  checks the review text
     */
    public PostVisitDialog(Frame owner, Restaurant restaurant, Moderator moderator) {
        super(owner, "How was " + restaurant.getName() + "?", true);
        this.moderator = moderator;
        this.choice = Choice.LATER;
        this.errorLabel = Theme.text(" ", Font.BOLD, 13, Theme.ERROR);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BACKGROUND);
        root.add(Theme.headerBar("How was " + restaurant.getName() + "?",
            "You said you'd eat here last time. Your rating tunes your recommendations."),
            BorderLayout.NORTH);

        JPanel body = Theme.card(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(4, 0, 4, 0);

        body.add(Theme.text("Your rating", Font.BOLD, 14, Theme.BLACK), c);
        starRating = new Theme.StarRating(0, true, 36);
        JLabel starWords = Theme.text(STAR_WORDS[0], Font.PLAIN, 14, Theme.GRAY);
        starRating.setOnChange(() -> {
            starWords.setText(STAR_WORDS[starRating.getStars()]);
            starWords.setForeground(Theme.ORANGE_DARK);
            errorLabel.setText(" ");
        });
        JPanel starRow = Theme.clear(new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)));
        starRow.add(starRating);
        starRow.add(javax.swing.Box.createHorizontalStrut(14));
        starRow.add(starWords);
        body.add(starRow, c);

        c.insets = new Insets(14, 0, 4, 0);
        body.add(Theme.text("Review (optional)", Font.BOLD, 14, Theme.BLACK), c);
        c.insets = new Insets(4, 0, 4, 0);
        reviewArea = new JTextArea(3, 32);
        reviewArea.setFont(Theme.font(Font.PLAIN, 14));
        reviewArea.setLineWrap(true);
        reviewArea.setWrapStyleWord(true);
        reviewArea.setBorder(new EmptyBorder(6, 8, 6, 8));
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
        body.add(reviewScroll, c);

        goBackBox = new JCheckBox("I'd go back here");
        goBackBox.setFont(Theme.font(Font.PLAIN, 14));
        goBackBox.setOpaque(false);
        c.insets = new Insets(10, 0, 4, 0);
        body.add(goBackBox, c);

        body.add(errorLabel, c);

        JPanel bodyHolder = new JPanel(new BorderLayout());
        bodyHolder.setBackground(Theme.BACKGROUND);
        bodyHolder.setBorder(new EmptyBorder(18, 18, 6, 18));
        bodyHolder.add(body, BorderLayout.CENTER);
        root.add(bodyHolder, BorderLayout.CENTER);

        JButton didntGo = Theme.button("I didn't go", Theme.ButtonStyle.GHOST);
        JButton later = Theme.button("Ask me later", Theme.ButtonStyle.SECONDARY);
        JButton submit = Theme.button("Submit rating", Theme.ButtonStyle.PRIMARY);
        didntGo.addActionListener(e -> close(Choice.DIDNT_GO));
        later.addActionListener(e -> close(Choice.LATER));
        submit.addActionListener(e -> submit());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setBackground(Theme.BACKGROUND);
        buttons.setBorder(new EmptyBorder(8, 18, 18, 18));
        buttons.add(didntGo);
        buttons.add(later);
        buttons.add(submit);
        root.add(buttons, BorderLayout.SOUTH);

        setContentPane(root);
        getRootPane().setDefaultButton(submit);
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    /**
     * @return what the user chose (LATER if they closed the window)
     */
    public Choice getChoice() {
        return choice;
    }

    /**
     * @return 1 to 5 if they submitted a rating
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

    /** Checks the answers; bad input shows a red message and keeps the dialog open. */
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
        close(Choice.SUBMITTED);
    }

    private void close(Choice picked) {
        choice = picked;
        dispose();
    }
}
